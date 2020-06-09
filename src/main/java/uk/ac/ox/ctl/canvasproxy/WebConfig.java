package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.RefreshOAuth2AuthorizedClient;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.web.method.annotation.OAuth2AccessTokenArgumentResolver;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.OAuth2AuthorizedClientArgumentResolver;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RefreshOAuth2AuthorizedClient oAuth2AuthorizedClientRepository;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private PrincipalClientIdResolver principalClientIdResolver;

    @Autowired
    private ServerProperties serverProperties;

    @Override
    public void addArgumentResolvers(
            List<HandlerMethodArgumentResolver> argumentResolvers) {
        {
            OAuth2AuthorizedClientArgumentResolver resolver = new OAuth2AuthorizedClientArgumentResolver(clientRegistrationRepository, oAuth2AuthorizedClientRepository);
            resolver.setPrincipalClientIdResolver(principalClientIdResolver);
            argumentResolvers.add(resolver);
        }
        {
            OAuth2AccessTokenArgumentResolver accessTokenArgumentResolver = new OAuth2AccessTokenArgumentResolver(clientRegistrationRepository, oAuth2AuthorizedClientRepository);
            accessTokenArgumentResolver.setPrincipalClientIdResolver(principalClientIdResolver);
            argumentResolvers.add(accessTokenArgumentResolver);
        }
    }

    @Bean
    public CustomErrorAttributes errorAttributes() {
        return new CustomErrorAttributes(this.serverProperties.getError().isIncludeException());
    }
}
