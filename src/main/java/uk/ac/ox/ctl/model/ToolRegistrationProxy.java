package uk.ac.ox.ctl.model;

import lombok.Data;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
// We do lots of lookups by clientId and want to ensure these hit an index.
@Table(indexes = {
        @Index(columnList = "clientId"),
        @Index(columnList = "registrationId", unique = true)
})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ToolRegistrationProxy extends ToolRegistration {

}
