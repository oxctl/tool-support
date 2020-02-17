package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/token")
public class TokenController {

    @GetMapping()
    public OAuth2AccessToken token(@RegisteredOAuth2AuthorizedClient("canvas") OAuth2AuthorizedClient client) {
        return client.getAccessToken();
    }

}
