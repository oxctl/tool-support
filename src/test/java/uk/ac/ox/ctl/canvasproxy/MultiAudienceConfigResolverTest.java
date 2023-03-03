package uk.ac.ox.ctl.canvasproxy;

import com.nimbusds.jose.util.Base64URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiAudienceConfigResolverTest {

    @Mock
    private ToolRepository toolRepository;

    private MultiAudienceConfigResolver createResolver(Map<String, AudienceConfiguration.LtiAudience> config) {
        return new MultiAudienceConfigResolver(toolRepository,
                new AudienceConfiguration(config)
        );
    }

    @Test
    public void testNoConfig() {
        MultiAudienceConfigResolver resolver = createResolver(emptyMap());
        assertNull(resolver.findProxyRegistration("audience"));
        assertNull(resolver.findIssuer("audience"));
        assertNull(resolver.findHmacSecret("audience"));
    }

    @Test
    public void testSimpleConfig() {
        MultiAudienceConfigResolver resolver = createResolver(singletonMap(
                "audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null)
        ));
        assertEquals("clientRegId", resolver.findProxyRegistration("audience"));
        assertNull(resolver.findProxyRegistration("notFound"));
    }

    @Test
    public void testMultipleAudiences() {
        MultiAudienceConfigResolver resolver = createResolver(singletonMap(
                "audience", new AudienceConfiguration.LtiAudience("clientRegId", null, null)
        ));

        assertEquals("clientRegId", resolver.findProxyRegistration("audience"));
        assertNull(resolver.findProxyRegistration("other4"));
    }

    @Test
    public void testDifferentMappings() {
        MultiAudienceConfigResolver resolver = createResolver(Map.of(
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId1", null, null),
                "audience2", new AudienceConfiguration.LtiAudience("clientRegId2", null, null)
        ));
        assertEquals("clientRegId2", resolver.findProxyRegistration("audience2"));
    }

    @Test
    public void testMapToSame() {
        // If multiple audiences map to a single clientRegId then this is ok as we know what to get for the user.
        MultiAudienceConfigResolver resolver = createResolver(Map.of(
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId", null, null), 
                "audience2", new AudienceConfiguration.LtiAudience("clientRegId", null, null)
        ));

        assertEquals("clientRegId", resolver.findProxyRegistration("audience2"));
    }

    @Test
    public void testDbLookup() {
        MultiAudienceConfigResolver resolver = createResolver(Map.of("" +
                "audience1", new AudienceConfiguration.LtiAudience("clientRegId", "http://issuer.test", Base64URL.encode("secret").toString())
        ));
        {
            // Check we load from config ok.
            assertEquals("clientRegId", resolver.findProxyRegistration("audience1"));
            assertEquals("http://issuer.test", resolver.findIssuer("audience1"));
            assertArrayEquals("secret".getBytes(), resolver.findHmacSecret("audience1"));
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

            assertEquals("dbClientRegId", resolver.findProxyRegistration("audience1"));
            assertEquals("http://db.issuer.test", resolver.findIssuer("audience1"));
            assertArrayEquals("db.secret".getBytes(), resolver.findHmacSecret("audience1"));
        }
    }

    @Test
    public void testNoProxyOnTool() {
        MultiAudienceConfigResolver resolver = new MultiAudienceConfigResolver(toolRepository, new AudienceConfiguration(Collections.emptyMap()));
        Tool tool = new Tool();
        when(toolRepository.findToolByLtiClientId("audience1")).thenReturn(Optional.of(tool));
        // When there's no proxy registration on the LTI we should just return null.

        assertNull(resolver.findProxyRegistration("audience1"));
    }

}