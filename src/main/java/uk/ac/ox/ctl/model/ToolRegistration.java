package uk.ac.ox.ctl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import uk.ac.ox.ctl.AuthenticationMethodJsonConverter;
import uk.ac.ox.ctl.AuthorizationGrantTypeJsonConverter;
import uk.ac.ox.ctl.ClientAuthenticationMethodJsonConverter;
import uk.ac.ox.ctl.repository.AuthenticationMethodDBConverter;
import uk.ac.ox.ctl.repository.AuthorizationGrantTypeDBConverter;
import uk.ac.ox.ctl.repository.ClientAuthenticationMethodDBConverter;
import uk.ac.ox.ctl.repository.StringSetConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This contains the data needed for a client registration object to work with Spring Security.
 * We have a specific version of this for LTI registrations and for Proxy registrations.
 * @see ToolRegistrationLti
 * @see ToolRegistrationProxy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
abstract public class ToolRegistration {
    
    @Id
    @Type(type = "uuid-char")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false )
    @MapsId()
    @ToString.Exclude // Stop recursive toString()
    @JoinColumn(name = "id")
    @JsonIgnore // stop loops
    private Tool tool;
    
    private String registrationId;
    private String clientName;
    private String clientId;
    private String clientSecret;
    @Convert(converter = ClientAuthenticationMethodDBConverter.class)
    @JsonDeserialize(converter = ClientAuthenticationMethodJsonConverter.Deserialize.class)
    @JsonSerialize(converter = ClientAuthenticationMethodJsonConverter.Serialize.class)
    private ClientAuthenticationMethod clientAuthenticationMethod;
    @Convert(converter = AuthorizationGrantTypeDBConverter.class)
    @JsonDeserialize(converter = AuthorizationGrantTypeJsonConverter.Deserialize.class)
    @JsonSerialize(converter = AuthorizationGrantTypeJsonConverter.Serialize.class)
    private AuthorizationGrantType authorizationGrantType;
    
    private String redirectUri;
    
    @Convert(converter = StringSetConverter.class)
    @Column(length = 8192)
    private Set<String> scopes = Collections.emptySet();
    
    @Embedded
    private ProviderDetails providerDetails = new ProviderDetails();

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderDetails {
        private String authorizationUri;
        private String tokenUri;
        private UserInfoEndpoint userInfoEndpoint = new UserInfoEndpoint();
        private String jwkSetUri;
        private String issuerUri;
        @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
        @Column(columnDefinition = "json")
        private Map<String, Object> configurationMetadata = Collections.emptyMap();
    }

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoEndpoint {
        private String uri;
        @Convert(converter = AuthenticationMethodDBConverter.class)
        @JsonDeserialize(converter = AuthenticationMethodJsonConverter.Deserialize.class)
        @JsonSerialize(converter = AuthenticationMethodJsonConverter.Serialize.class)
        private AuthenticationMethod authenticationMethod;
        private String userNameAttributeName;
    }
    
    
}
