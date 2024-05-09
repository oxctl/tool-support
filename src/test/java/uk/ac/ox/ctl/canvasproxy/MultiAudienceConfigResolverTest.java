package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiAudienceConfigResolverTest {

    @Mock
    private ToolRepository toolRepository;

    @Test
    public void testNoConfig() {
        MultiAudienceConfigResolver resolver = new MultiAudienceConfigResolver(toolRepository);
        assertNull(resolver.findProxyRegistration("audience"));
        assertNull(resolver.findIssuer("audience"));
        assertNull(resolver.findHmacSecret("audience"));
    }

    @Test
    public void testFindProxyRegistration() {
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("clientRegId");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId("audience")).thenReturn(Optional.of(tool));
        MultiAudienceConfigResolver resolver = new MultiAudienceConfigResolver(toolRepository);
        assertEquals("clientRegId", resolver.findProxyRegistration("audience"));
        assertNull(resolver.findProxyRegistration("notFound"));
    }

    @Test
    public void testNoProxyOnTool() {
        MultiAudienceConfigResolver resolver = new MultiAudienceConfigResolver(toolRepository);
        Tool tool = new Tool();
        when(toolRepository.findToolByLtiClientId("audience1")).thenReturn(Optional.of(tool));
        // When there's no proxy registration on the LTI we should just return null.

        assertNull(resolver.findProxyRegistration("audience1"));
    }

    @Test
    public void testFindIssuer() {
        Tool tool = new Tool();
        tool.setIssuer("issuer");
        when(toolRepository.findToolByLtiClientId("audience")).thenReturn(Optional.of(tool));
        MultiAudienceConfigResolver resolver = new MultiAudienceConfigResolver(toolRepository);
        assertEquals("issuer", resolver.findIssuer("audience"));
        assertNull(resolver.findIssuer("notFound"));
    }

}