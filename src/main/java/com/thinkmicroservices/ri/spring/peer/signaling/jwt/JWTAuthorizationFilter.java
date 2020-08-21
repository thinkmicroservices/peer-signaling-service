package com.thinkmicroservices.ri.spring.peer.signaling.jwt;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author cwoodward
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j

public class JWTAuthorizationFilter implements Filter {

    /**
     * message returned when the supplied token has expired.
     */
    protected static final String TOKEN_EXPIRED_MESSAGE = "Token Expired";
    /**
     * message returned when the no token is present
     */
    protected static final String TOKEN_MISSING_MESSAGE = "Token Missing";

    protected static final String TOKEN_ROLES_INSUFFICIENT_MESSAGE = "Token Insufficient Privileges";

    /* we store the authorization token in the sec-websocket-protocol
    enumeration, since the websocket spec, doesnt support arbitrary headers.
     */
    private static final String SEC_WEBSOCKET_PROTOCOL_HEADER = "sec-websocket-protocol";
    /*
    the authentication token should be in the second protocol value (index = 1) in 
    the sec-websocket-protocol header.
     */
    private int AUTHENTICATION_TOKEN_INDEX = 1;

    @Autowired
    private JWTService jwtService;

    /**
     * filters incoming requests and ensures a token is provided and has not
     * expired. The token is added to the request prior to dispatch to the next
     * filter.
     *
     * @param request
     * @param response
     * @param chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        log.debug("invoking JWTAuthorizationFilter...>");
        checkJWTServiceAvailable(request);
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestMethod = httpRequest.getMethod();
        log.debug("requestMethod={}", requestMethod);
        String requestURI = httpRequest.getRequestURI();
        log.debug("requestURI={}", requestURI);
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            log.debug("HEADER {}:{}", headerName, headerValue);
        }
        /*if (true) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_MISSING_MESSAGE);
            return;
        }
         */

        // this is an inelegant hack to skip authentication
        // for specific URIs
        if (requestURI.startsWith("/actuator/prometheus")) {
            chain.doFilter(request, response);
            return;
        }
        String jwtAuthenticationToken = null;

    
        
        Enumeration<String> secWebSocketProtocols = httpRequest.getHeaders(SEC_WEBSOCKET_PROTOCOL_HEADER);

        if (secWebSocketProtocols.hasMoreElements()) {
            String protocolHeaders = secWebSocketProtocols.nextElement();
            log.debug("sec-websocket-protocol headers: {}", protocolHeaders);
            String[] protocolHeaderArray = protocolHeaders.split(",");
            log.debug("sec-websocket-protocol headers size: {}", protocolHeaderArray.length);
            
            if (protocolHeaderArray.length< 2) {
                log.debug("insufficient web-socket-protocol header values");
                // no sec-websocket-protocol header values present
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_MISSING_MESSAGE);
                return;
            }
            log.debug("sec-websocket-protocol headers[0]:{}", protocolHeaderArray[0]);
            log.debug("sec-websocket-protocol headers[1]:{}", protocolHeaderArray[1]);
            // we expect the authentication token to be the second value
            jwtAuthenticationToken = protocolHeaderArray[AUTHENTICATION_TOKEN_INDEX];
        } else {
            log.debug("no sec-websocket-protocol values missing");
            // no sec-websocket-protocol header values present
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_MISSING_MESSAGE);
            return;

        }

        log.debug("websocket authentication token:{}", jwtAuthenticationToken);
       
        log.debug("requestURI={}", requestURI);
        List<String> rolesRequired = JWTRoleTable.getRequiredRolesByUriPath(requestURI, requestMethod);
        log.debug("required roles for {}={}", requestURI, rolesRequired);
        
        if (rolesRequired.size() > 0) {

            // get the token
            JWT jwt = jwtService.decodeJWT(jwtAuthenticationToken);
            log.debug("JWT:{}",jwt);
           
            // if no token present send an error
            if (jwt == null) {

                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_MISSING_MESSAGE);
                return;
            }

            // if the token has expired send an error
            if (jwt.isTokenExpired()) {
                log.debug("uri:{},token expired", httpRequest.getRequestURI());
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_EXPIRED_MESSAGE);
                return;
            }
           
            // if token is missing required roles send an error
            if (!jwt.hasAllRoles(rolesRequired)) {
                log.debug("uri:{},insufficient roles", httpRequest.getRequestURI());
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, TOKEN_ROLES_INSUFFICIENT_MESSAGE);
                return;
            }
           
        } 
        
        chain.doFilter(request, response);
        
    }

   

    public void checkJWTServiceAvailable(ServletRequest request) {
        log.debug("checking JWTService");
        // this is hack to get the jwtService in the Filter
        if (jwtService == null) {
            ServletContext servletContext = request.getServletContext();
            WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            jwtService = webApplicationContext.getBean(JWTService.class);
        }
        log.debug("JWTService=>{}", jwtService);
    }
}
