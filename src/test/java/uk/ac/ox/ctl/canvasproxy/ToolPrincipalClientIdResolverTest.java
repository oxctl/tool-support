package uk.ac.ox.ctl.canvasproxy;

import com.nimbusds.jose.util.Base64URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ToolPrincipalClientIdResolverTest {

    @Mock
    private ToolRepository toolRepository;

    private ToolPrincipalClientIdResolver createResolver(Map<String, AudienceConfiguration.LtiAudience> config) {
        return new ToolPrincipalClientIdResolver(
                new MultiAudienceConfigResolver(toolRepository,
                        new AudienceConfiguration(config)
                )
        );
    }


    private PersistableJwtAuthenticationToken createToken(String... audiences) {
        Jwt jwt = Jwt.withTokenValue("value").header("header", "value").audience(Arrays.asList(audiences)).build();
        return new PersistableJwtAuthenticationToken(jwt);
    }

    @Test
    public void testNoConfig() {
        ToolPrincipalClientIdResolver resolver = createResolver(emptyMap());
        assertNull(resolver.findClientId(createToken("audience")));
    }

    @Test
    public void testSimpleConfig() {
        ToolPrincipalClientIdResolver resolver = createResolver(
                singletonMap("audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null))
        );
        assertEquals("clientRegId", resolver.findClientId(createToken("audience")));
        assertNull(resolver.findClientId(createToken("notFound")));
    }

    @Test
    public void testMultipleAudiences() {
        ToolPrincipalClientIdResolver resolver = createResolver(
                singletonMap("audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null))
        );
        assertEquals("clientRegId", resolver.findClientId(createToken("other1", "other2", "audience")));
        assertNull(resolver.findClientId(createToken("other3", "other4")));
    }

    @Test
    public void testDifferentMappings() {
        ToolPrincipalClientIdResolver resolver = createResolver(Map.of(
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId1", null, null),
                "audience2", new AudienceConfiguration.LtiAudience("clientRegId2", null, null)
        ));
        assertThrows(IllegalStateException.class, () -> resolver.findClientId(createToken("audience1", "audience2")));
    }

    @Test
    public void testMapToSame() {
        // If multiple audiences map to a single clientRegId then this is ok as we know what to get for the user.
        ToolPrincipalClientIdResolver resolver = createResolver(Map.of(
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId", null, null),
                "audience2", new AudienceConfiguration.LtiAudience("clientRegId", null, null)
        ));
        assertEquals("clientRegId", resolver.findClientId(createToken("audience1", "audience2")));
    }

    @Test
    public void testDbLookup() {
        ToolPrincipalClientIdResolver resolver = createResolver(Map.of(
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId", "http://issuer.test", Base64URL.encode("secret").toString())
        ));
        {
            // Check we load from config ok.
            assertEquals("clientRegId", resolver.findClientId(createToken("audience1")));
//            assertEquals("http://issuer.test", resolver.findIssuer("audience1"));
//            assertArrayEquals("secret".getBytes(), resolver.findHmacSecret("audience1"));
        }
        // Now add a DB config that overrides the static config
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("dbClientRegId");
        Tool tool = new Tool();
        tool.setIssuer("http://db.issuer.test");
        tool.setSecret(Base64URL.encode("db.secret").toString());
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId("audience1")).thenReturn(Optional.of(tool));
        {
            // Now check that we get back the values from the DB.
            assertEquals("dbClientRegId", resolver.findClientId(createToken("audience1")));
//            assertEquals("http://db.issuer.test", resolver.findIssuer("audience1"));
//            assertArrayEquals("db.secret".getBytes(), resolver.findHmacSecret("audience1"));
        }
    }

    @Test
    public void testNoProxyOnTool() {
        ToolPrincipalClientIdResolver resolver = createResolver(emptyMap());
        Tool tool = new Tool();
        when(toolRepository.findToolByLtiClientId("audience1")).thenReturn(Optional.of(tool));
        // When there's no proxy registration on the LTI we should just return null.
        assertNull(resolver.findClientId(createToken("audience1")));
    }


}