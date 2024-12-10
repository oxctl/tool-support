package uk.ac.ox.ctl.ltiauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.lti13.KeyPairService;
import uk.ac.ox.ctl.lti13.SingleKeyPairService;
import uk.ac.ox.ctl.lti13.TokenRetriever;
import uk.ac.ox.ctl.lti13.nrps.NamesRoleService;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@Configuration
public class Lti13Configuration {

    private final Logger log = LoggerFactory.getLogger(Lti13Configuration.class);

    @Autowired
    private KeyPairLoadingService keyPairLoadingService;

    /**
     * The password for the JWK key file.
     */
    @Value("${lti.jwk.password:store-pass}")
    private String storePassword;

    /***
     * The ID of the key in the JKS file.
     */
    @Value("${lti.jwk.id:lti-jwt-id}")
    private String jwtId;

    @Bean
    public JWTSigner jwtSigner(KeyPairService keyPairService, @Value("${lti.jwk.id:lti-jwt-id}") String keyId) {
        return new JWTSigner(keyPairService, keyId);
    }
    @Bean
    public NamesRoleService namesRoleService(@Qualifier("lti") ClientRegistrationRepository clientRegistrationRepository, TokenRetriever tokenRetriever) {
        return new NamesRoleService(clientRegistrationRepository, tokenRetriever);
    }
    


    @Bean
    public TokenRetriever tokenRetriever(KeyPairService keyPairService) {
        return new TokenRetriever(keyPairService);
    }
    
    @Bean
    public NRPSService nrpsService(TokenRetriever tokenRetriever) {
        return new NRPSService(tokenRetriever);
    }

    @Bean
    public KeyPairService keyPairService(KeyPair keyPair) {
        return new SingleKeyPairService(keyPair, jwtId);
    }

    @Bean
    public JWTService jwtService(JWTSigner signer, JWTStore store, LtiSettings ltiSettings) {
        return new JWTService(signer, store, ltiSettings);
    }

    @Bean
    public JWTStore jwtStore(@Value("${jwt.store.duration:10m}") Duration duration) {
        return new JWTStore(duration);
    }

    @Bean
    public KeyPair keyPair() {
        return keyPairLoadingService.loadKeyPair(storePassword);
    }

    @Bean
    public JWKSet jwkSet() {
        RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey) keyPair().getPublic())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(jwtId);
        return new JWKSet(builder.build());
    }
}
