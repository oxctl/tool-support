package uk.ac.ox.ctl.ltiauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import uk.ac.ox.ctl.lti13.utils.KeyStoreKeyFactory;

import java.security.KeyPair;

/**
 * Service to generate a key pair from jks file in AWS Secrets Manager.
 */
@Service
@Profile("aws")
@Lazy
public class AwsKeyPairLoadingService implements KeyPairLoadingService {

    private final Logger log = LoggerFactory.getLogger(AwsKeyPairLoadingService.class);

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    /***
     * The ID of the secret whose value contains the binary-stored JKS file.
     */
    @Value("${jks.aws.secret.id:}")
    private String jwtAwsSecretId;

    @Override
    public KeyPair loadKeyPair(String storePassword){
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(jwtAwsSecretId).build();
            byte[] jksFile = secretsManagerClient.getSecretValue(valueRequest).secretBinary().asByteArray();
            Resource resource = new ByteArrayResource(jksFile);
            KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(resource, storePassword.toCharArray());
            log.info("Loaded key from AWS Secret: "+ jwtAwsSecretId);
            return ksFactory.getKeyPair("jwt");
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Failed to retrieve jks file from Secrets Manager using jwk secret id: " + jwtAwsSecretId, e);
        }
    }
}