/*
 * Copyright (C) 2011-2014 ShenZhen iBOXPAY Information Technology Co.,Ltd.
 * 
 * All right reserved.
 * 
 * This software is the confidential and proprietary
 * information of iBoxPay Company of China. 
 * ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only
 * in accordance with the terms of the contract agreement 
 * you entered into with iBoxpay inc.
 *
 */

package org.springframework.session.web.http;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.session.Session;
import org.springframework.util.StringUtils;

/**
 * The class CookieAndParamHttpSessionStrategy.
 *
 * Description: 
 *
 * @author: yinyuesheng
 * @since: 2015年8月15日	
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */

public class CookieAddParamHttpSessionStrategy implements MultiHttpSessionStrategy, HttpSessionManager {

    private CookieHttpSessionStrategy cookieStrategy;

    static final String DEFAULT_SESSION_PARAM_NAME = "SESSION";

    private String sessionParam = DEFAULT_SESSION_PARAM_NAME;

    @Override
    public String getRequestedSessionId(HttpServletRequest request) {
        String sid = cookieStrategy.getRequestedSessionId(request);
        if (StringUtils.isEmpty(sid)) {
            sid = request.getParameter(sessionParam);
        }
        return sid;
    }

    @Override
    public void onNewSession(Session session, HttpServletRequest request, HttpServletResponse response) {
        cookieStrategy.onNewSession(session, request, response);
    }

    @Override
    public void onInvalidateSession(HttpServletRequest request, HttpServletResponse response) {
        cookieStrategy.onInvalidateSession(request, response);
    }

    @Override
    public HttpServletRequest wrapRequest(HttpServletRequest request, HttpServletResponse response) {
        return cookieStrategy.wrapRequest(request, response);
    }

    @Override
    public HttpServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse response) {
        return cookieStrategy.wrapResponse(request, response);
    }

    @Override
    public String getCurrentSessionAlias(HttpServletRequest request) {
        return cookieStrategy.getCurrentSessionAlias(request);

    }

    @Override
    public Map<String, String> getSessionIds(HttpServletRequest request) {
        return cookieStrategy.getSessionIds(request);
    }

    @Override
    public String encodeURL(String url, String sessionAlias) {
        return cookieStrategy.encodeURL(url, sessionAlias);
    }

    @Override
    public String getNewSessionAlias(HttpServletRequest request) {
        return cookieStrategy.getNewSessionAlias(request);
    }

    public CookieHttpSessionStrategy getCookieStrategy() {
        return cookieStrategy;
    }

    public void setCookieStrategy(CookieHttpSessionStrategy cookieStrategy) {
        this.cookieStrategy = cookieStrategy;
    }

    public String getSessionParam() {
        return sessionParam;
    }

    public void setSessionParam(String sessionParam) {
        this.sessionParam = sessionParam;
    }

}
