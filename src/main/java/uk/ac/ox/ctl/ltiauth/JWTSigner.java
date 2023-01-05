package uk.ac.ox.ctl.ltiauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ox.ctl.lti13.KeyPairService;

import java.security.KeyPair;

public class JWTSigner {

    private final Logger log = LoggerFactory.getLogger(JWTSigner.class);

    private final KeyPairService keyPairService;
    
    private final String keyId;

    public JWTSigner(KeyPairService keyPairService, String keyId) {
        this.keyPairService = keyPairService;
        this.keyId = keyId;
    }

    public SignedJWT signJWT(String clientRegistrationId, JWTClaimsSet claims) throws JOSEException {
        KeyPair keyPair = keyPairService.getKeyPair(clientRegistrationId);
        if (keyPair == null) {
            throw new NullPointerException(
                    "Failed to get keypair for client registration: "+ clientRegistrationId
            );
        }
        RSASSASigner signer = new RSASSASigner(keyPair.getPrivate());
        // We don't have to include a key ID, but if we want to use the JWK URL we do.
        JWSHeader jwt = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(keyId)
                .build();
        SignedJWT signedJWT = new SignedJWT(jwt, claims);
        signedJWT.sign(signer);

        if (log.isDebugEnabled()) {
            log.debug("Created signed token: {}", signedJWT.serialize());
        }

        return signedJWT;
    }


}
