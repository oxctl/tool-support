package uk.ac.ox.ctl.canvasproxy;

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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ToolPrincipalClientIdResolverTest {

    @Mock
    private ToolRepository toolRepository;

    private ToolPrincipalClientIdResolver createResolver() {
        return new ToolPrincipalClientIdResolver(
                new MultiAudienceConfigResolver(toolRepository)
        );
    }


    private PersistableJwtAuthenticationToken createToken(String... audiences) {
        Jwt jwt = Jwt.withTokenValue("value").header("header", "value").audience(Arrays.asList(audiences)).build();
        return new PersistableJwtAuthenticationToken(jwt);
    }

    @Test
    public void testNoConfig() {
        ToolPrincipalClientIdResolver resolver = createResolver();
        assertThrows(ProxyConfigException.class, () -> resolver.findClientId(createToken("audience")));
    }

    @Test
    public void testSimpleConfig() {
        ToolPrincipalClientIdResolver resolver = createResolver();
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("clientRegId");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId("audience")).thenReturn(Optional.of(tool));

        assertEquals("clientRegId", resolver.findClientId(createToken("audience")));
        assertThrows(ProxyConfigException.class, () -> resolver.findClientId(createToken("notFound")));
    }

    @Test
    public void testNoProxyOnTool() {
        ToolPrincipalClientIdResolver resolver = createResolver();
        Tool tool = new Tool();
        when(toolRepository.findToolByLtiClientId("audience1")).thenReturn(Optional.of(tool));
        // When there's no proxy registration on the LTI we should just return null.
        assertThrows(ProxyConfigException.class, () -> resolver.findClientId(createToken("audience1")));
    }


}