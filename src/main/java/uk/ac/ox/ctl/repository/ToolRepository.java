package uk.ac.ox.ctl.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.ctl.model.Tool;

import javax.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;


public interface ToolRepository extends CrudRepository<Tool, UUID> {
    
    /**
     * This allows us to check to see if the origin should be allowed.
     * @param origin The origin the request 
     */
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    boolean existsToolByOrigins(String origin);

    @EntityGraph(attributePaths = {"lti", "proxy"})
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<Tool> findToolByLtiClientId(String id);

    @EntityGraph(attributePaths = {"lti", "proxy"})
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<Tool> findToolByProxyClientId(String id);

    @EntityGraph(attributePaths = {"lti", "proxy"})
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<Tool> findToolByLtiRegistrationId(String id);

    @EntityGraph(attributePaths = {"lti", "proxy"})
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<Tool> findToolByProxyRegistrationId(String id);

    @EntityGraph(attributePaths = {"lti", "proxy"})
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true") })
    Optional<Tool> findToolByLtiClientIdOrProxyClientIdOrLtiRegistrationIdOrProxyRegistrationId(String ltiClientId, String proxyClientId, String ltiRegistrationId, String proxyRegistrationId);

    default Optional<Tool> findByClientOrRegistrationIds(String ltiClientId, String proxyClientId, String ltiRegistrationId, String proxyRegistrationId) {
        return findToolByLtiClientIdOrProxyClientIdOrLtiRegistrationIdOrProxyRegistrationId(ltiClientId, proxyClientId, ltiRegistrationId, proxyRegistrationId);
    }

    Iterable<Tool> findToolByLtiIdNotNull();

}
