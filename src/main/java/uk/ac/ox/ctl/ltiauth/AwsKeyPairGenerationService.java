package uk.ac.ox.ctl.ltiauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.Filter;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
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

    @Override
    public KeyPair generateKeyPair(String jwtAwsSecretId, String storePassword, String location){
        try {
            ListSecretsRequest listRequest = ListSecretsRequest.builder()
                    .filters(Filter.builder().key("name").values(jwtAwsSecretId).build()).build();
            if (secretsManagerClient.listSecrets(listRequest) != null
                    && !secretsManagerClient.listSecrets(listRequest).secretList().isEmpty()) {
                GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(jwtAwsSecretId).build();
                byte[] jksFile = secretsManagerClient.getSecretValue(valueRequest).secretBinary().asByteArray();
                Resource resource = new ByteArrayResource(jksFile);
                KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(resource, storePassword.toCharArray());
                log.info("Loaded key from " + jksFile);
                return ksFactory.getKeyPair("jwt");
            }
            else {
                log.info("Generated a keypair, this shouldn't be used in production.");
                return KeyPairGenerator.getInstance("RSA").generateKeyPair();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate keypair");
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Failed to retrieve jks file from Secrets Manager using jwk secret id: " + jwtAwsSecretId, e);
        }
    }
}