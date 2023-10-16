package uk.ac.ox.ctl.admin;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(properties = "spring.security.user.password=pass1234", controllers = AdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.(canvasproxy|ltiauth)\\..*"))
@Import({AdminWebSecurity.class})
@ImportAutoConfiguration(exclude = {
        OAuth2ClientAutoConfiguration.class
})
@Transactional
@AutoConfigureCache
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@Slf4j
public class AdminControllerWebTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ToolRepository repository;

    @Test
    public void testCreate() throws Exception {
        String json = mvc.perform(post("/admin/tools")
            .with(httpBasic("user", "pass1234"))
            .contentType(APPLICATION_JSON)
            .content("""
                {"secret" : "secret", "issuer" :  "issuer", "origins": ["origin1", "origin2"], "nrpsAllowedRoles": ["role1", "role2"]}
             """))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"secret": "secret", "issuer": "issuer", "origins": ["origin1", "origin2"], "nrpsAllowedRoles": ["role1", "role2"]}
            """))
            .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();

        String id = JsonPath.read(json, "$.id");
        assertNotNull(id);
        UUID uuid = UUID.fromString(id);

        assertThat(repository.findById(uuid)).isNotEmpty();
    }
    
    @Test
    public void testFinding() throws Exception {
        {
            Tool tool = new Tool();
            ToolRegistrationLti lti = new ToolRegistrationLti();
            lti.setClientId("ltiClientId");
            lti.setRegistrationId("ltiRegistrationId");
            lti.setRedirectUri("http://server.test");
            lti.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
            lti.setAuthorizationGrantType(AuthorizationGrantType.IMPLICIT);
            tool.setLti(lti);
            ToolRegistrationProxy proxy = new ToolRegistrationProxy();
            proxy.setClientId("proxyClientId");
            proxy.setRegistrationId("proxyRegistrationId");
            proxy.setRedirectUri("http://server.test");
            proxy.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
            proxy.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            tool.setProxy(proxy);
            entityManager.persist(tool);
            entityManager.flush();
        }

        mvc.perform(get("/admin/tools/ltiClientId:ltiClientId")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                ).andExpect(status().isOk());

        mvc.perform(get("/admin/tools/ltiRegistrationId:ltiRegistrationId")
                .with(httpBasic("user", "pass1234"))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());

        mvc.perform(get("/admin/tools/proxyClientId:proxyClientId")
                .with(httpBasic("user", "pass1234"))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());

        mvc.perform(get("/admin/tools/proxyRegistrationId:proxyRegistrationId")
                .with(httpBasic("user", "pass1234"))
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk());
        
    }

    @Test
    public void testCreateWithLti() throws Exception {
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "lti": { "registrationId":  "reg123", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lti").exists())
                .andExpect(jsonPath("$.lti.clientAuthenticationMethod").value("client_secret_basic"))
                ;

        entityManager.flush();
    }

    @Test
    public void testRemoveProxy() throws Exception {
        String id;
        {
            Tool tool = new Tool();
            tool.setSign(false);
            ToolRegistrationProxy proxy = new ToolRegistrationProxy();
            proxy.setRegistrationId("reg123");
            tool.setProxy(proxy);
            id = repository.save(tool).getId().toString();
        }
        entityManager.flush();
        entityManager.clear();
        mvc.perform(put("/admin/tools/"+ id)
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer"}
             """))
                .andExpect(status().isOk())
        ;
        entityManager.flush();
    }

    @Test
    public void testCreateNoAuthentication() throws Exception {
        mvc.perform(post("/admin/tools"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testCreateWrongUser() throws Exception {
        mvc.perform(post("/admin/tools")
            .with(httpBasic("wrong_user", "pass1234")))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testCreateWrongPassword() throws Exception {
        mvc.perform(post("/admin/tools")
            .with(httpBasic("user", "wrong_password")))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testGetAll() throws Exception {
        Tool tool1 = new Tool();
        tool1.setNrpsAllowedRoles(Set.of("role", "role2"));
        tool1.setOrigins(Set.of("origin1", "origin2"));
        Tool tool2 = new Tool();
        tool2.setNrpsAllowedRoles(Collections.emptySet());
        tool2.setOrigins(Collections.emptySet());
        entityManager.persist(tool1);
        entityManager.persist(tool2);
        entityManager.flush();
        entityManager.clear();

        String json = mvc.perform(get("/admin/tools")
            .with(httpBasic("user", "pass1234")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String id1 = JsonPath.read(json, "$[0].id");
        String id2 = JsonPath.read(json, "$[1].id");
        assertNotNull(id1);
        assertNotNull(id2);

        UUID uuid1 = UUID.fromString(id1);
        UUID uuid2 = UUID.fromString(id2);

        assertThat(repository.findById(uuid1)).isNotEmpty();
        assertThat(repository.findById(uuid2)).isNotEmpty();
    }

    @Test
    public void testGetById() throws Exception {
        Tool tool1 = new Tool();
        tool1.setNrpsAllowedRoles(Set.of("role", "role2"));
        tool1.setOrigins(Set.of("origin1", "origin2"));
        entityManager.persist(tool1);
        entityManager.flush();
        entityManager.clear();

        String json = mvc.perform(get("/admin/tools/" + tool1.getId())
            .with(httpBasic("user", "pass1234")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(json, "$.id");
        assertNotNull(id);
        UUID uuid = UUID.fromString(id);
        assertEquals(tool1.getId(), uuid);
    }

    @Test
    public void testUpdate() throws Exception {
        Tool tool = new Tool();

        tool.setOrigins(List.of("origin1", "origin2"));
        tool.setSign(false);
        tool.setSecret("secret");
        tool.setIssuer("issuer");
        tool.setNrpsAllowedRoles(Set.of("role1", "role2"));

        entityManager.persist(tool);
        entityManager.flush();
        entityManager.clear();

        String json = mvc.perform(put("/admin/tools/" + tool.getId())
            .with(httpBasic("user", "pass1234"))
            .contentType(APPLICATION_JSON)
            .content("""
                {"secret" : "newSecret", "issuer" :  "newIssuer", "origins": ["origin3", "origin4"], "nrpsAllowedRoles": ["role3", "role4"], "sign":  "true"}
            """))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                {"secret": "newSecret", "issuer": "newIssuer", "origins": ["origin3", "origin4"], "nrpsAllowedRoles": ["role3", "role4"], "sign": true}
            """))
            .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();

        String id = JsonPath.read(json, "$.id");
        assertNotNull(id);
        UUID uuid = UUID.fromString(id);

        assertEquals(uuid, tool.getId());
    }

    @Test
    public void testUpdateIdNotFound() throws Exception{
        mvc.perform(put("/admin/tools/1")
            .with(httpBasic("user", "pass1234")))
            .andExpect(status().is4xxClientError());
    }

    @Test
    public void testDelete() throws Exception {
        Tool tool1 = new Tool();
        Tool tool2 = new Tool();
        tool2.setNrpsAllowedRoles(Collections.emptySet());
        tool2.setOrigins(Collections.emptySet());
        entityManager.persist(tool1);
        entityManager.persist(tool2);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(delete("/admin/tools/" + tool1.getId())
            .with(httpBasic("user", "pass1234")))
            .andExpect(status().isNoContent())
            .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(tool1.getId())).isEmpty();
        assertThat(repository.findById(tool2.getId())).isNotEmpty();
    }

    @Test
    public void testCreateWithExistingIds() throws Exception {

        {
            Tool tool = new Tool();
            ToolRegistrationLti lti = new ToolRegistrationLti();
            lti.setClientId("existing-lti-client-id");
            lti.setRegistrationId("existing-lti-registration-id");
            lti.setRedirectUri("http://server.test");
            lti.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
            lti.setAuthorizationGrantType(AuthorizationGrantType.IMPLICIT);
            tool.setLti(lti);
            ToolRegistrationProxy proxy = new ToolRegistrationProxy();
            proxy.setClientId("existing-proxy-client-id");
            proxy.setRegistrationId("existing-proxy-registration-id");
            proxy.setRedirectUri("http://server.test");
            proxy.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
            proxy.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            tool.setProxy(proxy);
            entityManager.persist(tool);
            entityManager.flush();
        }

        // Create a new tool with existing registrationId in the LTI
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "lti": { "clientId": "new-client-id", "registrationId":  "existing-lti-registration-id", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().is4xxClientError());

        // Create a new tool with existing clientId in the LTI
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "lti": { "clientId": "existing-lti-client-id", "registrationId":  "new-registration-id", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().is4xxClientError())
                ;

        // Create a new tool with existing registrationId in the Proxy
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "proxy": { "clientId": "new-client-id", "registrationId":  "existing-proxy-registration-id", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().is4xxClientError())
                ;

        // Create a new tool with existing registrationId in the Proxy
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "proxy": { "clientId": "existing-proxy-client-id", "registrationId":  "new-registration-id", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().is4xxClientError())
                ;

        // Create a new tool with new data
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "proxy": { "clientId": "new-client-id", "registrationId":  "new-registration-id", "clientAuthenticationMethod": "client_secret_basic" } , "lti": { "clientId": "new-lti-client-id", "registrationId":  "new-lti-registration-id", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().isOk())
                ;

        // Create a new tool with new data and lti only
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "lti": { "clientId": "new-lti-client-id-2", "registrationId":  "new-lti-registration-id-2", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().isOk())
                ;

        // Create a new tool with new data and proxy only.
        mvc.perform(post("/admin/tools")
                        .with(httpBasic("user", "pass1234"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                {"secret" : "secret", "issuer" :  "issuer", "proxy": { "clientId": "new-proxy-client-id-2", "registrationId":  "new-proxy-registration-id-2", "clientAuthenticationMethod": "client_secret_basic" }}
             """))
                .andExpect(status().isOk())
                ;
    }

}
