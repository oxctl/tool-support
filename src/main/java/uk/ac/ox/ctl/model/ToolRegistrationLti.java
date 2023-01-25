package uk.ac.ox.ctl.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
// We do lots of lookups by clientId and want to ensure these hit an index.
@Table(indexes = {
        @Index(columnList = "clientId"),
        @Index(columnList = "registrationId", unique = true)
})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ToolRegistrationLti extends ToolRegistration {
    

}
