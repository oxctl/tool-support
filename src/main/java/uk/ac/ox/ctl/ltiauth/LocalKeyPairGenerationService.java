package uk.ac.ox.ctl.ltiauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.ac.ox.ctl.lti13.utils.KeyStoreKeyFactory;

import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 *  * Service to generate a key pair from a local jks file.
 */
@Service
@Profile("local")
@Lazy
public class LocalKeyPairGenerationService implements KeyPairGenerationService{

    private final Logger log = LoggerFactory.getLogger(LocalKeyPairGenerationService.class);

    /**
     * The location of the local JWK key file (if running locally).
     */
    @Value("${lti.jwk.location:config/jwk.jks}")
    private String location;

    @Override
    public KeyPair generateKeyPair(String storePassword){
        try {
            Resource resource = new FileUrlResource(location);
            if (resource.exists()) {
                KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(resource, storePassword.toCharArray());
                log.info("Loaded key from "+ location);
                return ksFactory.getKeyPair("jwt");
            } else {
                log.info("Generated a keypair, this shouldn't be used in production.");
                return KeyPairGenerator.getInstance("RSA").generateKeyPair();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate keypair");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed retrieve jks file from local store " + location);
        }
    }
}