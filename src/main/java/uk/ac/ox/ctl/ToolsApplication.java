package uk.ac.ox.ctl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uk.ac.ox.ctl.ltiauth.LtiSettings;
import uk.ac.ox.ctl.ltiauth.controller.AllowedRoles;

// We shouldn't be required to exclude this, but I was finding that the detection of the WebSecurityFilter wasn't
// working and it was trying to create Beans it shouldn't
@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@EnableConfigurationProperties({
		// LTI Config
		IssuerConfiguration.class, AllowedRoles.class, LtiSettings.class})
public class ToolsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToolsApplication.class, args);
	}

}
