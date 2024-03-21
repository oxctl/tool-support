package uk.ac.ox.ctl.ltiauth;

import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Base64;
import java.util.Optional;

public class MultiAudienceConfigResolver implements AudienceConfigResolver {
    
    private final ToolRepository toolRepository;

    public MultiAudienceConfigResolver(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    public byte[] findHmacSecret(String audience) {
        // When reading from the properties we use Base64Url encoding.
        // When reading from the DB we use Base64 encoding.
        // This will catch someone out, but hopefully we get rid of it at some point.
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
    public String findLtiRegistration(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(tool -> (tool.getLti() != null) ? tool.getLti().getRegistrationId() : null)
                .orElse(null);
    }
}
