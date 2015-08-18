# spring-session-codis
提供spring-session中,codis存储会话信息

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xmlns:aop="http://www.springframework.org/schema/aop" xmlns:task="http://www.springframework.org/schema/task"
	xsi:schemaLocation="http://www.springframework.org/schema/beans 
       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
       http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context-3.0.xsd  
       http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-3.0.xsd  
       http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd">
	<context:property-placeholder location="classpath:session.properties"
		ignore-unresolvable="true" ignore-resource-not-found="true" order="2"
		system-properties-mode="NEVER" />
	<bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
		<property name="maxTotal" value="${jedispoolconfig.maxTotal}" />
		<property name="maxIdle" value="${jedispoolconfig.maxIdle}" />
		<property name="minIdle" value="${jedispoolconfig.minIdle}" />
		<property name="testWhileIdle" value="${jedispoolconfig.testWhileIdle}" />
		<property name="minEvictableIdleTimeMillis"
			value="${jedispoolconfig.minEvictableIdleTimeMillis}" />
		<property name="timeBetweenEvictionRunsMillis"
			value="${jedispoolconfig.timeBetweenEvictionRunsMillis}" />
		<property name="numTestsPerEvictionRun" value="${jedispoolconfig.numTestsPerEvictionRun}" />
	</bean>
	<bean id="jedisPool" class="com.wandoulabs.jodis.RoundRobinJedisPool"
		destroy-method="close">
		<constructor-arg index="0" type="java.lang.String"
			value="${jedispool.zkAddr}" />
		<constructor-arg index="1" type="int"
			value="${jedispool.zkSessionTimeoutMs}" />
		<constructor-arg index="2" type="java.lang.String"
			value="${jedispool.zkPath}" />
		<constructor-arg index="3"
			type="redis.clients.jedis.JedisPoolConfig" ref="jedisPoolConfig" />
	</bean>
	<bean id="codisSessionRepository"
		class="org.springframework.session.data.codis.CodisOperationsSessionRepository"
		init-method="init">
		<constructor-arg ref="jedisPool"></constructor-arg>
		<property name="defaultMaxInactiveInterval" value="${session.maxInactiveInterval}" />
	</bean>
	<bean id="cookieHttpSessionStrategy" name="cookieHttpSessionStrategy"
		class="org.springframework.session.web.http.CookieHttpSessionStrategy">
		<property name="cookieName" value="${session.cookieName}" />
	</bean>
	<bean id="cookieAddParamHttpSessionStrategy" name="cookieAddParamHttpSessionStrategy"
		class="org.springframework.session.web.http.CookieAddParamHttpSessionStrategy">
		<property name="sessionParam" value="${session.cookieName}" />
		<property name="cookieStrategy" ref="cookieHttpSessionStrategy" />
	</bean>
	<bean id="springSession" name="springSession"
		class="org.springframework.session.web.http.SessionRepositoryFilter">
		<constructor-arg ref="codisSessionRepository"></constructor-arg>
		<property name="httpSessionStrategy" ref="cookieAddParamHttpSessionStrategy" />
	</bean>
	<context:annotation-config />
	<context:component-scan base-package="org.springframework.session.data.codis" />
	<task:annotation-driven scheduler="sessionScheduler"
		mode="proxy" />
	<task:scheduler id="sessionScheduler" pool-size="${scheduler.poolsize}" />
</beans>


jedispoolconfig.maxTotal=50
jedispoolconfig.maxIdle=10
jedispoolconfig.minIdle=3
jedispoolconfig.testWhileIdle=true
jedispoolconfig.minEvictableIdleTimeMillis=60000
jedispoolconfig.timeBetweenEvictionRunsMillis=30000
jedispoolconfig.numTestsPerEvictionRun=3

jedispool.zkAddr=localhost:2181
jedispool.zkSessionTimeoutMs=30000
jedispool.zkPath=/zk/codis/db_risk/proxy

session.maxInactiveInterval=3600
session.cookieName=jsessionid

scheduler.poolsize=10
