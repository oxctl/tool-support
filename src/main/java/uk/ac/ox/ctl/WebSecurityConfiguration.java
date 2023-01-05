package uk.ac.ox.ctl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import uk.ac.ox.ctl.repository.ToolRepository;
import uk.ac.ox.ctl.service.ToolCorsConfigurationSource;

import java.util.Arrays;

@Configuration
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

}
