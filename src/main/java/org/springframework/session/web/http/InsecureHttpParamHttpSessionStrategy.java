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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.session.Session;

/**
 * The class InsecureHttpParamHttpSessionStrategy.
 *
 * Description: 
 *
 * @author: yinyuesheng
 * @since: 2015年8月15日	
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */

public class InsecureHttpParamHttpSessionStrategy implements MultiHttpSessionStrategy {

    static final String DEFAULT_SESSION_ALIAS_PARAM_NAME = "SESSION";

    private String sessionParam = DEFAULT_SESSION_ALIAS_PARAM_NAME;

    public String getRequestedSessionId(HttpServletRequest request) {
        return request.getParameter(sessionParam);
    }

    public void onNewSession(Session session, HttpServletRequest request, HttpServletResponse response) {
       /* request.setAttribute(sessionParam,session.getId());
        System.out.println(request.getAttribute(sessionParam));*/
    }

    public void onInvalidateSession(HttpServletRequest request, HttpServletResponse response) {
       //request.removeAttribute(sessionParam);
    }

    public HttpServletRequest wrapRequest(HttpServletRequest request, HttpServletResponse response) {
        return request;
    }

    public HttpServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse response) {
        return new InsecureResposneWrapper(response, request);
    }

    class InsecureResposneWrapper extends HttpServletResponseWrapper {

        private final HttpServletRequest request;

        public InsecureResposneWrapper(HttpServletResponse response, HttpServletRequest request) {
            super(response);
            this.request = request;
        }

        @Override
        public String encodeRedirectURL(String url) {
            url = super.encodeRedirectURL(url);
            // update to handle if ? exists already, session id is null, etc
            //return url + "?session=" + getRequestedSessionId(request);
            return InsecureHttpParamHttpSessionStrategy.this.encodeURL(url, getRequestedSessionId(request));
        }

        @Override
        public String encodeURL(String url) {
            url = super.encodeURL(url);
            // update to handle if ? exists already, session id is null, etc
            //return url + "?session=" + getRequestedSessionId(request);
            return InsecureHttpParamHttpSessionStrategy.this.encodeURL(url, getRequestedSessionId(request));
        }
    }

    public String encodeURL(String url, String sessionValues) {
        String encodedSessionsessionValues = urlEncode(sessionValues);
        int queryStart = url.indexOf("?");
        if (queryStart < 0) {
            return url + "?" + sessionParam + "=" + encodedSessionsessionValues;
        }
        String path = url.substring(0, queryStart);
        String query = url.substring(queryStart + 1, url.length());
        if (Pattern.compile("((^|&)" + sessionParam + "=)([^&]+)?").matcher(query).find()) {
            String replacement = "$1" + encodedSessionsessionValues;
            query = query.replaceFirst("((^|&)" + sessionParam + "=)([^&]+)?", replacement);
        } else if (url.endsWith(query)) {
            if (!(query.endsWith("&") || query.length() == 0)) {
                query += "&";
            }
            query += sessionParam + "=" + encodedSessionsessionValues;
        }
        return path + "?" + query;
    }

    public static void main(String[] arg) {
        InsecureHttpParamHttpSessionStrategy s = new InsecureHttpParamHttpSessionStrategy();
        String url = "http://www.iboxpay.com/?a=123&b=678&SESSION=110-990-099aaa&c=00";
        System.out.println(s.encodeURL(url, "110-990-099"));
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSessionParam() {
        return sessionParam;
    }

    public void setSessionParam(String sessionParam) {
        this.sessionParam = sessionParam;
    }

}
