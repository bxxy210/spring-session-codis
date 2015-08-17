/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.session.data.codis;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.ExpiringSession;

import redis.clients.jedis.Jedis;

import com.wandoulabs.jodis.JedisResourcePool;

/**
 * A strategy for expiring {@link RedisSession} instances. This performs two
 * operations:
 *
 * Redis has no guarantees of when an expired session event will be fired. In
 * order to ensure expired session events are processed in a timely fashion the
 * expiration (rounded to the nearest minute) is mapped to all the sessions that
 * expire at that time. Whenever {@link #cleanExpiredSessions()} is invoked, the
 * sessions for the previous minute are then accessed to ensure they are deleted if expired.
 *
 * In some instances the {@link #cleanExpiredSessions()} method may not be not
 * invoked for a specific time. For example, this may happen when a server is
 * restarted. To account for this, the expiration on the Redis session is also set.
 *
 * @author Rob Winch
 * @since 1.0
 */
final class CodisSessionExpirationPolicy {

    private static final Log logger = LogFactory.getLog(CodisSessionExpirationPolicy.class);

    /**
     * The prefix for each key of the Redis Hash representing a single session. The suffix is the unique session id.
     */
    static final String EXPIRATION_BOUNDED_HASH_KEY_PREFIX = "spring:session:expirations:";

    //private final RedisOperations<String,ExpiringSession> sessionRedisOperations = null;

    //private final RedisOperations<String,String> expirationRedisOperations = null;
    private final JedisResourcePool jedisPool;

    private final RedisSerializer<String> keySerializer;

    private final RedisSerializer<Object> valueSerializer;;

    public CodisSessionExpirationPolicy(JedisResourcePool jedisPool, RedisSerializer<String> keySerializer, RedisSerializer<Object> valueSerializer) {
        this.jedisPool = jedisPool;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    public void onDelete(ExpiringSession session) {
        long toExpire = roundUpToNextMinute(expiresInMillis(session));
        String expireKey = getExpirationKey(toExpire);
        //expirationRedisOperations.boundSetOps(expireKey).remove(session.getId());
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.srem(expireKey, session.getId());
        }
    }

    public void onExpirationUpdated(Long originalExpirationTimeInMilli, ExpiringSession session) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (originalExpirationTimeInMilli != null) {
                long originalRoundedUp = roundUpToNextMinute(originalExpirationTimeInMilli);
                String expireKey = getExpirationKey(originalRoundedUp);
                //expirationRedisOperations.boundSetOps(expireKey).remove(session.getId());
                jedis.srem(expireKey, session.getId());
            }
            long toExpire = roundUpToNextMinute(expiresInMillis(session));
            String expireKey = getExpirationKey(toExpire);
            //BoundSetOperations<String, String> expireOperations = expirationRedisOperations.boundSetOps(expireKey);
            //expireOperations.add(session.getId());
            //jedisPool.getResource().smembers(expireKey).add(session.getId());
            jedis.sadd(expireKey, session.getId());
            long sessionExpireInSeconds = session.getMaxInactiveIntervalInSeconds();
            String sessionKey = getSessionKey(session.getId());
            //expireOperations.expire(sessionExpireInSeconds + 60, TimeUnit.SECONDS);
            jedis.expire(expireKey, (int) (sessionExpireInSeconds + 60));
            //sessionRedisOperations.boundHashOps(sessionKey).expire(sessionExpireInSeconds, TimeUnit.SECONDS);
            jedis.expire(sessionKey, (int) sessionExpireInSeconds);
        }
    }

    String getExpirationKey(long expires) {
        return EXPIRATION_BOUNDED_HASH_KEY_PREFIX + expires;
    }

    String getSessionKey(String sessionId) {
        return CodisOperationsSessionRepository.BOUNDED_HASH_KEY_PREFIX + sessionId;
    }

    public void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        long prevMin = roundDownMinute(now);

        if (logger.isDebugEnabled()) {
            logger.debug("Cleaning up sessions expiring at " + new Date(prevMin));
        }
        String expirationKey = getExpirationKey(prevMin);
        //Set<String> sessionsToExpire = expirationRedisOperations.boundSetOps(expirationKey).members();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> sessionsToExpire = jedis.smembers(expirationKey);
            //expirationRedisOperations.delete(expirationKey);
            jedis.del(expirationKey);
            for (String session : sessionsToExpire) {
                String sessionKey = getSessionKey(session);
                touch(sessionKey);
            }
        }
    }

    /**
     * By trying to access the session we only trigger a deletion if it the TTL is expired. This is done to handle
     * https://github.com/spring-projects/spring-session/issues/93
     *
     * @param key
     */
    private void touch(String key) {
        //sessionRedisOperations.hasKey(key);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.exists(key);
        }
    }

    static long expiresInMillis(ExpiringSession session) {
        int maxInactiveInSeconds = session.getMaxInactiveIntervalInSeconds();
        long lastAccessedTimeInMillis = session.getLastAccessedTime();
        return lastAccessedTimeInMillis + TimeUnit.SECONDS.toMillis(maxInactiveInSeconds);
    }

    static long roundUpToNextMinute(long timeInMs) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timeInMs);
        date.add(Calendar.MINUTE, 1);
        date.clear(Calendar.SECOND);
        date.clear(Calendar.MILLISECOND);
        return date.getTimeInMillis();
    }

    public static void main(String[] arg) {
        for (int i = 0; i < 100; i++) {
            long t = System.currentTimeMillis();
            System.out.println(roundDownMinute(t));
            System.out.println(t);
            System.out.println(roundUpToNextMinute(t));
            System.out.println("---------------------------");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {

            }
        }
    }

    static long roundDownMinute(long timeInMs) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timeInMs);
        date.clear(Calendar.SECOND);
        date.clear(Calendar.MILLISECOND);
        return date.getTimeInMillis();
    }
}
