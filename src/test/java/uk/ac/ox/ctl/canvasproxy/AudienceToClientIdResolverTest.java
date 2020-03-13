package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.Map;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AudienceToClientIdResolverTest {

    @Test
    public void testNoConfig() {
        AudienceToClientIdResolver resolver = new AudienceToClientIdResolver(emptyMap());
        assertNull(resolver.findClientId(createToken("audience")));
    }

    @Test
    public void testSimpleConfig() {
        AudienceToClientIdResolver resolver = new AudienceToClientIdResolver(singletonMap("audience", "clientRegId"));
        assertEquals("clientRegId", resolver.findClientId(createToken("audience")));
        assertNull(resolver.findClientId(createToken("notFound")));
    }

    @Test
    public void testMultipleAudiences() {
        AudienceToClientIdResolver resolver = new AudienceToClientIdResolver(singletonMap("audience", "clientRegId"));
        assertEquals("clientRegId", resolver.findClientId(createToken("other1", "other2", "audience")));
        assertNull(resolver.findClientId(createToken("other3", "other4")));
    }

    @Test
    public void testDifferentMappings() {
        AudienceToClientIdResolver resolver = new AudienceToClientIdResolver(Map.of("audience1", "clientRegId1", "audience2", "clientRegId2"));
        assertThrows(IllegalStateException.class, () -> resolver.findClientId(createToken("audience1", "audience2")));
    }

    @Test
    public void testMapToSame() {
        // If multiple audiences map to a single clientRegId then this is ok as we know what to get for the user.
        AudienceToClientIdResolver resolver = new AudienceToClientIdResolver(Map.of("audience1", "clientRegId", "audience2", "clientRegId"));
        assertEquals("clientRegId", resolver.findClientId(createToken("audience1", "audience2")));
    }

    private JwtAuthenticationToken createToken(String... audiences) {
        Jwt jwt = Jwt.withTokenValue("value").header("header", "value").audience(Arrays.<String>asList(audiences)).build();
        return new JwtAuthenticationToken(jwt);
    }

}