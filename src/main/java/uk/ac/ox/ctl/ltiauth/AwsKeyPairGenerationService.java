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
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import uk.ac.ox.ctl.lti13.utils.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Service to generate a key pair from jks file in AWS Secrets Manager.
 */
@Service
@Profile("!local")
@Lazy
public class AwsKeyPairGenerationService implements KeyPairGenerationService{

    private final Logger log = LoggerFactory.getLogger(AwsKeyPairGenerationService.class);

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    /***
     * The ID of the secret whose value contains the binary-stored JKS file.
     */
    @Value("${jks.aws.secret.id:}")
    private String jwtAwsSecretId;

    @Override
    public KeyPair generateKeyPair(String storePassword){
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(jwtAwsSecretId).build();
            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(valueRequest);
            if (getSecretValueResponse!=null){
                byte[] jksFile = getSecretValueResponse.secretBinary().asByteArray();
                Resource resource = new ByteArrayResource(jksFile);
                KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(resource, storePassword.toCharArray());
                log.info("Loaded key from AWS Secret: "+ jwtAwsSecretId);
                return ksFactory.getKeyPair("jwt");
            }
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Failed to retrieve jks file from Secrets Manager using jwk secret id: " + jwtAwsSecretId, e);
        }
        return null;
    }
}