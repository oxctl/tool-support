package uk.ac.ox.ctl.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import uk.ac.ox.ctl.repository.StringSetConverter;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
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
    @GeneratedValue
    @Type(type = "uuid-char")
    @Column(length = 36)
    private UUID id;

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
     * Should we re-sign the JWT when performing an LTI launch?
     * This is a boolean so if it's null then we can fallback to the global default.
     */
    private Boolean sign;

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
