package uk.ac.ox.ctl.ltiauth;

import org.springframework.stereotype.Service;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.List;

/**
 * This looks up the roles that are allowed to use the Names and Roles provisioning service (NRPS)
 * from the client.
 */
public class AllowedRolesService {
    
    private final ToolRepository toolRepository;

    public AllowedRolesService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    /**
     * Is this client ID allows to use the NRPS service?
     * @param clientId The client ID.
     * @param roles The list of roles the current user has.
     * @return true if they can.
     */
    public boolean isNRPSAllowed(String clientId, List<String> roles) {
        return toolRepository.findToolByLtiClientId(clientId)
                .map(tool -> tool.getNrpsAllowedRoles().stream().anyMatch(roles::contains))
                .orElse(false);
    }
}
