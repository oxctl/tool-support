package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JWTServiceTest {

    @Nested
    public class CheckingClaims {
        // These tests are to make sure we aren't going to blow up with bad token values.
        // Checking the logger does the right thing isn't worth the complexity
        
        private JWTService service;
        private OAuth2User user;

        @BeforeEach
        public void setUp() {
            service = new JWTService(mock(JWTSigner.class), mock(JWTStore.class), mock(LtiSettings.class));
            user = mock(OAuth2User.class);
        }
        
        @Test
        public void noMap() {
            OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, null, "test");
            service.checkForNullCustomValues(token);
        }

        @Test
        public void goodClaim() {
            when(user.getAttribute("https://purl.imsglobal.org/spec/lti/claim/custom")).thenReturn(singletonMap("key", "value"));
            OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, null, "test");
            service.checkForNullCustomValues(token);
        }

        @Test
        public void nullClaim() {
            when(user.getAttribute("https://purl.imsglobal.org/spec/lti/claim/custom")).thenReturn(singletonMap("key", null));
            OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(user, null, "test");
            service.checkForNullCustomValues(token);
        }
    }

}