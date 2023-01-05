package uk.ac.ox.ctl.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;

/**
 * @deprecated Just use ToolRepository
 */
public interface ToolRegistrationProxyRepository extends CrudRepository<ToolRegistrationProxy, String> {
}
