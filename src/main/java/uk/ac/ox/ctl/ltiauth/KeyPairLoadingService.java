package uk.ac.ox.ctl.ltiauth;

import java.security.KeyPair;

/**
 *  * Service to load the keypair from either a file or secrets manager.
 */
public interface KeyPairLoadingService {

    KeyPair loadKeyPair(String storePassword);
}
