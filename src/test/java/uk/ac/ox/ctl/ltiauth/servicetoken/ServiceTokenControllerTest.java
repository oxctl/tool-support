package uk.ac.ox.ctl.ltiauth.servicetoken;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.PlainHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.TokenRetriever;
import uk.ac.ox.ctl.ltiauth.ClientRegistrationService;
import uk.ac.ox.ctl.ltiauth.MultiAudienceConfigResolver;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import static javax.xml.crypto.dsig.SignatureMethod.HMAC_SHA256;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServiceTokenController.class,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.ltiauth\\.servicetoken\\..*"),
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.canvasproxy\\..*")
)
class ServiceTokenControllerTest {

    @MockBean
    private ClientRegistrationService clientRegistrationService;

    @MockBean
    private TokenRetriever tokenRetriever;

    @MockBean
    private MultiAudienceConfigResolver multiAudienceConfigResolver;

    @Autowired
    private MockMvc mvc;

    private final byte[] secret = "012345678901234567890123456789012".getBytes(StandardCharsets.UTF_8);
    SecretKeySpec key = new SecretKeySpec(secret, HMAC_SHA256);

    @BeforeEach
    public void setUp() throws JOSEException {
        when(multiAudienceConfigResolver.findHmacSecret("test")).thenReturn(secret);
        // Client registration
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("test")
                .authorizationGrantType(new AuthorizationGrantType("lti"))
                .build();
        when(clientRegistrationService.findByClientId("test")).thenReturn(clientRegistration);
        // Token response from Canvas
        OAuth2AccessTokenResponse oauth2TokenResponse = OAuth2AccessTokenResponse.withToken("token")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(3600)
                .build();
        when(tokenRetriever.getToken(clientRegistration)).thenReturn(oauth2TokenResponse);

        // Secret Lookup
        when(multiAudienceConfigResolver.findHmacSecret("test")).thenReturn(secret);
    }

    @Test
    public void noToken() throws Exception {
        mvc.perform(post("/service-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void wrongMethod() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims().build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void unsignedToken() throws Exception {
        PlainHeader header = new PlainHeader();
        JWTClaimsSet claims = jwtClaims().build();
        PlainJWT jwt = new PlainJWT(header, claims);

        mvc.perform(post("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void expiredToken() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims()
                .expirationTime(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void wrongIssuer() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims()
                .issuer("wrong")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void noSecret() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims()
                .issuer("test")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));
        // Override the default secret
        when(multiAudienceConfigResolver.findHmacSecret("test")).thenReturn(null);

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void wrongAudience() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims()
                .audience("wrong")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void wrongSecret() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims()
                .audience("wrong")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        byte[] wrongSecret = new byte[32];
        Arrays.fill(wrongSecret, (byte) 0);
        jwt.sign(new MACSigner(new SecretKeySpec(wrongSecret, HMAC_SHA256)));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }

    @Test
    public void wrongAlgorithm() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.RS256);
        JWTClaimsSet claims = jwtClaims()
                .audience("wrong")
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);

        // Test with an RSA key that isn't trusted
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        PrivateKey key = keyGen.generateKeyPair().getPrivate();
        jwt.sign(new RSASSASigner(key));

        mvc.perform(get("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
        ;
    }


    @Test
    public void goodToken() throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claims = jwtClaims().build();
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(key));

        mvc.perform(post("/service-token").header("Authorization", "Bearer " + jwt.serialize()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isString())
                .andExpect(jsonPath("$.expires").isString())
                .andExpect(header().doesNotExist("Cookie"))
        ;
    }

    private static JWTClaimsSet.Builder jwtClaims() {
        return new JWTClaimsSet.Builder()
                .audience("test")
                .issuer("test")
                .notBeforeTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
    }

}