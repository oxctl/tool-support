package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import uk.ac.ox.ctl.canvasproxy.jwt.IssuerConfiguration;

@SpringBootApplication
@EnableConfigurationProperties({AudienceToClientIdResolver.class, IssuerConfiguration.class})
public class CanvasProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvasProxyApplication.class, args);
    }

}
