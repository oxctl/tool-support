package uk.ac.ox.ctl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import uk.ac.ox.ctl.repository.ToolRepository;
import uk.ac.ox.ctl.service.ToolCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {
    
    private final Logger log = LoggerFactory.getLogger(WebSecurityConfiguration.class);

    @Bean("corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource(ToolRepository toolRepository, @Value("${tool.origins:}") String[] origins) {
        if (Arrays.asList(origins).contains("*")) {
            log.warn("CORS origins allows any endpoints, not suitable for production.");
        } else {
            if (origins.length > 0) {
                log.info("CORS configured for {} origins.", origins.length);
            }
        }
        return new ToolCorsConfigurationSource(toolRepository, origins);
    }


    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public SecurityFilterChain finalSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(
                    "/",
                    "/index.html",
                    "/css/**",
                    "/images/**",
                    "/resources/**",
                    "/error"
                )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
