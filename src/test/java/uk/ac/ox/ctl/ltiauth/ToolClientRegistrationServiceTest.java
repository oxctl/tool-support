package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.LTIAuthorizationGrantType;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
class ToolClientRegistrationServiceTest {
    
    @Autowired
    private TestEntityManager entityManager;

    @MockBean
    private SecretsManagerClient secretsManagerClient;
    
    @Autowired
    private ToolRepository toolRepository;
    
    private ToolClientRegistrationService service;
    
    @BeforeEach
    private void setUp() {
        service = new ToolClientRegistrationService(toolRepository);
    }
    
    @Test
    public void testFindByClientIdMissing() {
        ToolClientRegistrationService service = new ToolClientRegistrationService(toolRepository);
        assertThat(service.findByClientId("not-found")).isNull();
    }
    
    @Test
    public void testFindByClientIdMinimal() {
        {
            Tool tool = new Tool();
            ToolRegistrationLti lti = new ToolRegistrationLti();
            lti.setRegistrationId("registration-id");
            lti.setClientId("12345");
            lti.setAuthorizationGrantType(LTIAuthorizationGrantType.IMPLICIT);
            lti.setRedirectUri("https://sample.test");
            lti.setScopes(Set.of("openid"));
            lti.getProviderDetails().setAuthorizationUri("https://sample.test/auth");
            lti.getProviderDetails().setTokenUri("https://sample.test/token");
            lti.getProviderDetails().setJwkSetUri("https://sample.test/jwks");
            tool.setLti(lti);
            entityManager.persist(tool);
            entityManager.flush();
            entityManager.clear();
        }
        ClientRegistration registration = service.findByClientId("12345");
        assertThat(registration).isNotNull();
        assertThat(registration.getRegistrationId()).isEqualTo("registration-id");
        assertThat(registration.getClientId()).isEqualTo("12345");
        assertThat(registration.getAuthorizationGrantType()).isEqualTo(LTIAuthorizationGrantType.IMPLICIT);
        assertThat(registration.getRedirectUri()).isEqualTo("https://sample.test");
        assertThat(registration.getScopes()).contains("openid");
        assertThat(registration.getProviderDetails()).isNotNull();
        assertThat(registration.getProviderDetails().getAuthorizationUri()).isEqualTo("https://sample.test/auth");
        assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo("https://sample.test/token");
        assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo("https://sample.test/jwks");
    }

}