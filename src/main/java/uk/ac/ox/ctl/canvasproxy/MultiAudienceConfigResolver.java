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
    private final AudienceConfiguration audienceConfiguration;

    public MultiAudienceConfigResolver(ToolRepository toolRepository, AudienceConfiguration audienceConfiguration) {
        this.toolRepository = toolRepository;
        this.audienceConfiguration = audienceConfiguration;
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
                .orElseGet(() -> audienceConfiguration.findHmacSecret(audience));
    }

    @Override
    public String findIssuer(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(Tool::getIssuer)
                .orElseGet(() -> audienceConfiguration.findIssuer(audience));
    }

    @Override
    public String findProxyRegistration(String audience) {
        Optional<Tool> optionalTool = toolRepository.findToolByLtiClientId(audience);
        return optionalTool
                .map(tool -> (tool.getProxy() != null) ? tool.getProxy().getRegistrationId() : null)
                .orElseGet(() -> audienceConfiguration.findProxyRegistration(audience));
    }
}
