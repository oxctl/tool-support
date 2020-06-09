package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RefreshOAuth2AuthorizedClient extends OAuth2AuthorizedClientRepository {

    <T extends OAuth2AuthorizedClient> T renewAccessToken(
            String clientRegistrationId, Authentication authentication, HttpServletRequest request, HttpServletResponse response);

    boolean needsRenewal(OAuth2AuthorizedClient oAuth2AuthorizedClient);
}
