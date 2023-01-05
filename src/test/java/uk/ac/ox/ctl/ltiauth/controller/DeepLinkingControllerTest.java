package uk.ac.ox.ctl.ltiauth.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.WebSecurityConfiguration;
import uk.ac.ox.ctl.canvasproxy.OAuth2Configuration;
import uk.ac.ox.ctl.canvasproxy.WebSecurity;
import uk.ac.ox.ctl.canvasproxy.jwt.ProxyJwtConfig;
import uk.ac.ox.ctl.ltiauth.Lti13Configuration;
import uk.ac.ox.ctl.ltiauth.TestClientRegistrationConfig;
import uk.ac.ox.ctl.ltiauth.ToolClientRegistrationService;
import uk.ac.ox.ctl.ltiauth.jwt.LtiJwtConfig;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ox.ctl.lti13.lti.Claims.*;
import static uk.ac.ox.ctl.ltiauth.controller.DeepLinkingController.*;

@WebMvcTest(controllers = DeepLinkingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.canvasproxy\\..*"))
@ImportAutoConfiguration(exclude = OAuth2ClientAutoConfiguration.class)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
@Import({Lti13Configuration.class, TestClientRegistrationConfig.class})
class DeepLinkingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testDenied() throws Exception {
        mvc
                .perform(post("/deep-linking/test").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBadJwt() throws Exception {
        // Just a random JWT
        String sampleJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0Ijo" +
                "xNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        mvc
                .perform(post("/deep-linking/test").header("Authorization", "Bearer " + sampleJwt).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNoClaims() throws Exception {
        JwtRequestPostProcessor jwt = jwt();
        mvc.perform(post("/deep-linking/test").with(jwt).content("{}"))
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    void testWrongAudience() throws Exception {
        JwtRequestPostProcessor jwt = jwt().jwt(
                builder ->  builder
                        .claim(MESSAGE_TYPE, "LtiDeepLinkingRequest")
                        .audience(Collections.singleton("does-not-exist"))
                        .build()
        );
        mvc.perform(post("/deep-linking/test").with(jwt).content("{}"))
                .andExpect(status().isBadRequest())
        ;
       
    }

    @Test
    void testWrongContent() throws Exception {
        JwtRequestPostProcessor jwt = jwt().jwt(
                builder ->  builder
                        .claim(MESSAGE_TYPE, "LtiDeepLinkingRequest")
                        .audience(Collections.singleton("1234"))
                        .issuer("http://example.com")
                        .build()
        );
        mvc.perform(post("/deep-linking/test").with(jwt).content("not a parsable JSON string"))
                .andExpect(status().is4xxClientError())
        ;
    }

    @Test
    void testNoItems() throws Exception {
        JwtRequestPostProcessor jwt = jwt().jwt(
                builder ->  builder
                        .claim(MESSAGE_TYPE, "LtiDeepLinkingRequest")
                        .audience(Collections.singleton("1234"))
                        .issuer("http://example.com")
                        .claim(LTI_DEPLOYMENT_ID, "deployment-1")
                        .claim(DL_CLAIM_DATA, "data")
                        .build()
        );
        String response = mvc.perform(post("/deep-linking/test").with(jwt).content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.jwt").isString())
                .andReturn().getResponse().getContentAsString();
        ;

        JSONObject json = new JSONParser(DEFAULT_PERMISSIVE_MODE).parse(response, JSONObject.class);
        JWTClaimsSet claims = SignedJWT.parse((String) json.get("jwt")).getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("1234");
        assertThat(claims.getAudience()).contains("http://example.com");
        assertThat(claims.getIssueTime()).isNotNull();
        assertThat(claims.getExpirationTime()).isNotNull();
        assertThat(claims.getStringClaim("azp")).isEqualTo("1234");
        
        assertThat(claims.getStringClaim(MESSAGE_TYPE)).isEqualTo("LtiDeepLinkingResponse");
        assertThat(claims.getStringClaim(LTI_VERSION)).isEqualTo("1.3.0");
        assertThat(claims.getStringClaim(LTI_DEPLOYMENT_ID)).isEqualTo("deployment-1");
        assertThat(claims.getStringClaim(DL_CLAIM_DATA)).isEqualTo("data");
        
        assertThat(claims.getJSONObjectClaim(DL_CLAIM_CONTENT_ITEMS)).isNull();
    }

    @Test
    void testCannotPassThrough() throws Exception {
        JwtRequestPostProcessor jwt = jwt().jwt(
                builder ->  builder
                        .claim(MESSAGE_TYPE, "LtiDeepLinkingRequest")
                        .audience(Collections.singleton("1234"))
                        .issuer("http://example.com")
                        .claim(LTI_DEPLOYMENT_ID, "deployment-1")
                        .claim(DL_CLAIM_DATA, "data")
                        .build()
        );
        // The content here shouldn't make it through into the JWT
        String response = mvc.perform(post("/deep-linking/test").with(jwt).content("{\"aud\": \"other\", \"new\": \"value\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.jwt").isString())
                .andReturn().getResponse().getContentAsString();
        ;

        JSONObject json = new JSONParser(DEFAULT_PERMISSIVE_MODE).parse(response, JSONObject.class);
        JWTClaimsSet claims = SignedJWT.parse((String) json.get("jwt")).getJWTClaimsSet();
        assertThat(claims.getAudience()).contains("http://example.com");
        assertThat(claims.getClaim("new")).isNull();
    }

    @Test
    void testSimpleItem() throws Exception {
        JwtRequestPostProcessor jwt = jwt().jwt(
                builder ->  builder
                        .claim(MESSAGE_TYPE, "LtiDeepLinkingRequest")
                        .audience(Collections.singleton("1234"))
                        .issuer("http://example.com")
                        .claim(LTI_DEPLOYMENT_ID, "deployment-1")
                        .claim(DL_CLAIM_DATA, "data")
                        .build()
        );

        // With JDK 17 can probably just make this a multiline string
        String deepLinking = Files.readString(Paths.get(getClass().getResource("/simple-deep-linking.json").toURI()));

        String response = mvc.perform(post("/deep-linking/test").with(jwt).content(deepLinking))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.jwt").isString())
                .andReturn().getResponse().getContentAsString();
        ;

        JSONObject json = new JSONParser(DEFAULT_PERMISSIVE_MODE).parse(response, JSONObject.class);
        JWTClaimsSet claims = SignedJWT.parse((String) json.get("jwt")).getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo("1234");
        assertThat(claims.getAudience()).contains("http://example.com");
        assertThat(claims.getIssueTime()).isNotNull();
        assertThat(claims.getExpirationTime()).isNotNull();
        assertThat(claims.getStringClaim("azp")).isEqualTo("1234");

        assertThat(claims.getStringClaim(MESSAGE_TYPE)).isEqualTo("LtiDeepLinkingResponse");
        assertThat(claims.getStringClaim(LTI_VERSION)).isEqualTo("1.3.0");
        assertThat(claims.getStringClaim(LTI_DEPLOYMENT_ID)).isEqualTo("deployment-1");
        assertThat(claims.getStringClaim(DL_CLAIM_DATA)).isEqualTo("data");

        assertThat(claims.getClaim(DL_CLAIM_CONTENT_ITEMS)).isNotNull();
        assertThat(claims.getStringClaim(DL_CLAIM_MSG)).isEqualTo("OK");
        
        assertThat(claims.getClaim(DL_CLAIM_ERRORLOG)).isNull();
        assertThat(claims.getClaim(DL_CLAIM_ERRORMSG)).isNull();
    }

}