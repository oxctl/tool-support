package uk.ac.ox.ctl.service;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import uk.ac.ox.ctl.repository.ToolRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This is needed so that we can load the allowed origins from the database.
 * It supports a static list of origins to allow migration of configuration over time.
 */
public class ToolCorsConfigurationSource implements CorsConfigurationSource {
    
    private final ToolRepository toolRepository;
    private final CorsConfiguration config;

    public ToolCorsConfigurationSource(ToolRepository toolRepository, String[] origins) {
        this.toolRepository = toolRepository;
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : origins) {
            config.addAllowedOrigin(origin);
        }
        addAllowed(config);
        this.config = config;
    }

    private void addAllowed(CorsConfiguration config) {
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("Link");
        config.addExposedHeader("WWW-Authenticate");
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration config = this.config;
        String origin = request.getHeader("origin");
        if (toolRepository.existsToolByOrigins(origin)) {
            config = new CorsConfiguration();
            config.addAllowedOrigin(origin);
            addAllowed(config);
        } 
        return config;
    }

}
