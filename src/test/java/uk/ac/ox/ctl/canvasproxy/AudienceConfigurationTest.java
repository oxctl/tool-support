package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;

import java.util.Arrays;
import java.util.Map;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AudienceConfigurationTest {
    
    @Test
    public void testNoConfig() {
        AudienceConfiguration resolver = new AudienceConfiguration(emptyMap());
        assertNull(resolver.findClientId(createToken("audience")));
    }

    @Test
    public void testSimpleConfig() {
        AudienceConfiguration resolver = new AudienceConfiguration(singletonMap("audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null)));
        assertEquals("clientRegId", resolver.findClientId(createToken("audience")));
        assertNull(resolver.findClientId(createToken("notFound")));
    }

    @Test
    public void testMultipleAudiences() {
        AudienceConfiguration resolver = new AudienceConfiguration(singletonMap("audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null)));
        assertEquals("clientRegId", resolver.findClientId(createToken("other1", "other2", "audience")));
        assertNull(resolver.findClientId(createToken("other3", "other4")));
    }

    @Test
    public void testDifferentMappings() {
        AudienceConfiguration resolver = new AudienceConfiguration(Map.of("audience1", new AudienceConfiguration.LtiAudience("clientRegId1", null, null), "audience2", new AudienceConfiguration.LtiAudience("clientRegId2", null, null)));
        assertThrows(IllegalStateException.class, () -> resolver.findClientId(createToken("audience1", "audience2")));
    }

    @Test
    public void testMapToSame() {
        // If multiple audiences map to a single clientRegId then this is ok as we know what to get for the user.
        AudienceConfiguration resolver = new AudienceConfiguration(Map.of("audience1", new AudienceConfiguration.LtiAudience("clientRegId", null, null), "audience2", new AudienceConfiguration.LtiAudience("clientRegId", null, null)));
        assertEquals("clientRegId", resolver.findClientId(createToken("audience1", "audience2")));
    }

    private PersistableJwtAuthenticationToken createToken(String... audiences) {
        Jwt jwt = Jwt.withTokenValue("value").header("header", "value").audience(Arrays.asList(audiences)).build();
        return new PersistableJwtAuthenticationToken(jwt);
    }

}