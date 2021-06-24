package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uk.ac.ox.ctl.canvasproxy.jwt.IssuerConfiguration;

@SpringBootApplication
@EnableConfigurationProperties({AudienceConfiguration.class, IssuerConfiguration.class})
public class CanvasProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvasProxyApplication.class, args);
    }

}
