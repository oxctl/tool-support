package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.OAuth2AuthorizedClientArgumentResolver;

import java.util.List;

@Configuration
public class ProxyWebMvcConfigurer implements WebMvcConfigurer {

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    // This is because we copied a whole load of spring stuff and so we need do our own resolving.
    argumentResolvers.add(
        new OAuth2AuthorizedClientArgumentResolver(
            clientRegistrationRepository, oAuth2AuthorizedClientRepository));
  }

}
