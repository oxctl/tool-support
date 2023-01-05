package uk.ac.ox.ctl.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ox.ctl.model.ToolRegistration;
import uk.ac.ox.ctl.model.ToolRegistrationLti;

/**
 * @deprecated Just use ToolRepository
 */
public interface ToolRegistrationLtiRepository extends CrudRepository<ToolRegistrationLti, String> {
}
