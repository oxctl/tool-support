package uk.ac.ox.ctl.canvasproxy;

import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Base64;
import java.util.Optional;

/**
 * This looks in the DB for config and only if we don't find it do we fallback to the 
 * file based config. This means that if the lookup in the DB succeeds but we don't have a
 * value for the lookup (eg no proxy for an LTI tool) we won't fallback to the files.
 */
public class MultiAudienceConfigResolver implements AudienceConfigResolver {
    
    private final ToolRepository toolRepository;

    public MultiAudienceConfigResolver(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    public byte[] findHmacSecret(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(Tool::getSecret)
                .map(secret -> Base64.getDecoder().decode(secret))
                .orElse(null);
    }

    @Override
    public String findIssuer(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(Tool::getIssuer)
                .orElse(null);
    }

    @Override
    public String findProxyRegistration(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(tool -> (tool.getProxy() != null) ? tool.getProxy().getRegistrationId() : null)
                .orElse(null);
    }
}
