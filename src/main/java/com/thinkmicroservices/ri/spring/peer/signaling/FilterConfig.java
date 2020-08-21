
package com.thinkmicroservices.ri.spring.peer.signaling;

 
import com.thinkmicroservices.ri.spring.peer.signaling.jwt.JWTAuthorizationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author cwoodward
 */
@Configuration
@Slf4j
public class FilterConfig {

    protected static final String URL_PATTERN_TELEMETRY= "/*";
 

    /**
     * 
     * @return 
     */
    @Bean
    public FilterRegistrationBean<JWTAuthorizationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JWTAuthorizationFilter> filterRegistrationBean
                = new FilterRegistrationBean<>(new JWTAuthorizationFilter());

        filterRegistrationBean.addUrlPatterns(URL_PATTERN_TELEMETRY);
        
        log.debug("JWTAuthorizationFilter patterns {}", filterRegistrationBean.getUrlPatterns());
        return filterRegistrationBean;
    }

}