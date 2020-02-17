package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
public class CanvasProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CanvasProxyApplication.class, args);
    }

}
