package uk.ac.ox.ctl.ltiauth;

import java.security.KeyPair;

/**
 *  * Service to generate a key pair.
 */
public interface KeyPairGenerationService {

    KeyPair generateKeyPair(String jwtAwsSecretId, String storePassword, String location);
}
