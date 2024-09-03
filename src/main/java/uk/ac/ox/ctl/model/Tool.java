package uk.ac.ox.ctl.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import uk.ac.ox.ctl.repository.StringSetConverter;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Overall configuration for a tool.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Tool {

    @Id
    @UuidGenerator
    @Column(length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JdbcType(VarcharJdbcType.class)
    private UUID id;

    public void setLti(ToolRegistrationLti lti) {
        if(lti != null){
            lti.setTool(this);
        }
        this.lti = lti;
    }

    public void setProxy(ToolRegistrationProxy proxy) {
        if(proxy != null){
            proxy.setTool(this);
        }
        this.proxy = proxy;
    }

    // This side has to be the non owning side so that we can join when loading entities
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tool")
    @PrimaryKeyJoinColumn
    @Fetch(FetchMode.JOIN)
    private ToolRegistrationLti lti;

    // This side has to be the non owning side so that we can join when loading entities
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tool")
    @PrimaryKeyJoinColumn
    private ToolRegistrationProxy proxy;
    
    /**
     * The origins that we allow.
     */
    @ElementCollection
    // We want to allow fast lookups of origins but they shouldn't be unique because multiple tools can be running
    // on the same origin.
    @CollectionTable(indexes = {@Index(columnList = "origin")})
    @Column(name="origin")
    private Collection<String> origins;

    /**
     * The secret that is used to sign JWT HMAC tokens to allow serverside components to use tokens without
     * having the user present.
     * This is Base64 encoded and so before signing a token needed to be decoded to a byte array.
     */
    private String secret;
    
    /**
     * The issuer of secret signed tokens.
     */
    private String issuer;

    /**
     * The roles that are allowed to use the NRPS service.
     */
    @Convert(converter = StringSetConverter.class)
    @Column(length = 1024)
    private Set<String> nrpsAllowedRoles;
}
