package uk.ac.ox.ctl.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.LTIAuthorizationGrantType;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ToolRepositoryTest {
    
    @Autowired
    private TestEntityManager testEntityManager;
    @Autowired
    private ToolRepository repository;
    
    @Test
    public void testRoundTrip() {
        {
            Tool tool = new Tool();
            repository.save(tool);
        }
    }
    
    @Test
    public void testValidOrigin() {
        String origin = "https://example.test";
        {
            // Not currently valid
            Assertions.assertFalse(repository.existsToolByOrigins(origin));
        }
        {
            // Add the valid tool
            Tool tool = new Tool();
            tool.setOrigins(Set.of(origin));
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            assertTrue(repository.existsToolByOrigins(origin));
        }
    }

    @Test
    public void testMultipleMatchingOrigins() {
        String origin = "https://example.test";
        {
            // Add the valid tool
            Tool tool = new Tool();
            tool.setOrigins(Set.of(origin));
            repository.save(tool);
        }
        {
            // Add the valid tool
            Tool tool = new Tool();
            tool.setOrigins(Set.of(origin));
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            assertTrue(repository.existsToolByOrigins(origin));
        }
    }
    
    @Test
    public void testFindByLtiClientId() {
        {
            Tool tool = new Tool();
            ToolRegistrationLti registration = new ToolRegistrationLti();
            registration.setClientId("1234");
            tool.setLti(registration);
            registration.setTool(tool);
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            repository.findToolByLtiClientId("1234").orElseThrow();
        }
    }

    @Test
    public void testFindByProxyClientId() {
        {
            Tool tool = new Tool();
            ToolRegistrationProxy registration = new ToolRegistrationProxy();
            registration.setClientId("1234");
            tool.setProxy(registration);
            registration.setTool(tool);
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            repository.findToolByProxyClientId("1234").orElseThrow();
        }
    }


    @Test
    public void testLtiAndProxy() {
        {
            // The same ID is used across the two tool registrations so check they aren't being stored
            // in the same table.
            Tool tool = new Tool();
            ToolRegistrationLti lti = new ToolRegistrationLti();
            lti.setClientId("1234");
            lti.setTool(tool);
            tool.setLti(lti);
            ToolRegistrationProxy proxy = new ToolRegistrationProxy();
            proxy.setClientId("5678");
            proxy.setTool(tool);
            tool.setProxy(proxy);
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
    }


    @Test
    public void testRoundTripToolRegistration() {
        UUID uuid;
        {
            ToolRegistrationLti registration = new ToolRegistrationLti();
            registration.setRegistrationId("test");
            registration.setScopes(Set.of("one", "two", "three"));
            registration.setAuthorizationGrantType(LTIAuthorizationGrantType.IMPLICIT);
            registration.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
            registration.getProviderDetails().setConfigurationMetadata(Map.of("key", "value"));
            
            Tool tool = new Tool();
            tool.setLti(registration);
            registration.setTool(tool);
            repository.save(tool);
            uuid = tool.getId();
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            ToolRegistrationLti registration = repository.findById(uuid).orElseThrow().getLti();
            assertThat(registration.getScopes()).contains("one", "two", "three");
            assertEquals(LTIAuthorizationGrantType.IMPLICIT, registration.getAuthorizationGrantType());
            assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, registration.getClientAuthenticationMethod());
            assertThat(registration.getProviderDetails().getConfigurationMetadata()).containsAllEntriesOf(Map.of("key", "value"));
        }
    }
    
    @Test
    public void testFindToolByLtiNotNull() {
        {
            Tool tool = new Tool();
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        assertThat(repository.findToolByLtiIdNotNull()).isEmpty();
        {
            // Create a tool with a LTI registration
            Tool tool = new Tool();
            ToolRegistrationLti registration = new ToolRegistrationLti();
            tool.setLti(registration);
            registration.setTool(tool);
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        assertThat(repository.findToolByLtiIdNotNull()).hasSize(1);
        {
            // Create a tool with a Proxy registration
            Tool tool = new Tool();
            ToolRegistrationProxy registration = new ToolRegistrationProxy();
            tool.setProxy(registration);
            registration.setTool(tool);
            repository.save(tool);
        }
        testEntityManager.flush();
        testEntityManager.clear();
        assertThat(repository.findToolByLtiIdNotNull()).hasSize(1);
    }


    @Test
    public void testToolWithLtiProxyDelete() {
        UUID id;
        {
            Tool tool = new Tool();
            ToolRegistrationProxy proxy = new ToolRegistrationProxy();
            proxy.setClientId("1234");
            tool.setProxy(proxy);
            proxy.setTool(tool);
            ToolRegistrationLti lti = new ToolRegistrationLti();
            lti.setClientId("5678");
            tool.setLti(lti);
            lti.setTool(tool);
            repository.save(tool);
            id = tool.getId();
        }
        testEntityManager.flush();
        testEntityManager.clear();
        {
            repository.deleteById(id);
        }
        testEntityManager.flush();
        testEntityManager.clear();
    }

}