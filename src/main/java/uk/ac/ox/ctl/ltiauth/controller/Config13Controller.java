package uk.ac.ox.ctl.ltiauth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.ac.ox.ctl.ltiauth.controller.lti13.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.ox.ctl.ltiauth.controller.lti13.Canvas13Extension.INSTRUCTURE;


// This is a leftover controller from when we just supported one application with this service.
// When we support configuring the service without restarts we should look at if we could make this controller
// work for all registrations.
@RestController
public class Config13Controller {

    private final JWKSet jwkSet;
    @Value("${spring.application.name:LTI Tool}")
    private String title;
    @Value("${lti.application.description:Tool description.}")
    private String description;
    @Value("${lti.jwk.id:lti-jwt-id}")
    private String jwtId;

    public Config13Controller(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

    @GetMapping("/config.json")
    public Lti13Config getConfig(HttpServletRequest request) {
        String urlPrefix = ServletUriComponentsBuilder.fromContextPath(request).toUriString();
        Canvas13Placement settingsPlacement = new Canvas13PlacementBuilder()
                .placement(Canvas13Placement.Placement.COURSE_SETTINGS_SUB_NAVIGATION)
                .canvasIconClass("icon-module")
                .enabled(true)
                .messageType(Canvas13Placement.MessageType.LtiResourceLinkRequest)
                .createCanvas13Placement();
        List<Canvas13Placement> placements = Arrays.asList(settingsPlacement);
        Canvas13Settings canvas13Settings = new Canvas13SettingsBuilder()
                .placements(placements)
                .createCanvas13Settings();
        Collection<Canvas13Extension> extensions = Collections.singleton(new Canvas13ExtensionBuilder()
                .platform(INSTRUCTURE)
                .domain(request.getServerName())
                .privacyLevel(Lti13Config.PrivacyLevel.ANONYMOUS)
                .settings(canvas13Settings)
                .createCanvas13Extension());
        Map<String, String> customFields = new HashMap<>();
        customFields.put("canvas_course_id", "$Canvas.course.id");
        customFields.put("canvas_root_account_global_id", "$Canvas.root_account.global_id");
        customFields.put("com_instructure_brand_config_json_url", "$com.instructure.brandConfigJSON.url");
        return new Lti13ConfigBuilder()
                .title(title)
                .description(description)
                .oidcInitiaionUrl(urlPrefix + "/lti/login_initiation/canvas")
                // This should be the frontend URL.
                .targetLinkUri(urlPrefix)
                .extensions(extensions)
                .publicJwk(jwkSet.getKeyByKeyId(jwtId).toPublicJWK().toJSONObject())
                .customFields(customFields)
                .createLti13Config();
    }

    @GetMapping("/cm-config.json")
    public Lti13Config courseManagement(HttpServletRequest request) {
        String urlPrefix = ServletUriComponentsBuilder.fromContextPath(request).toUriString();
        Canvas13Placement settingsPlacement = new Canvas13PlacementBuilder()
                .placement(Canvas13Placement.Placement.ACCOUNT_NAVIGATION)
                .enabled(true)
                .messageType(Canvas13Placement.MessageType.LtiResourceLinkRequest)
                .createCanvas13Placement();
        List<Canvas13Placement> placements = Arrays.asList(settingsPlacement);
        Canvas13Settings canvas13Settings = new Canvas13SettingsBuilder()
                .placements(placements)
                .createCanvas13Settings();
        Collection<Canvas13Extension> extensions = Collections.singleton(new Canvas13ExtensionBuilder()
                .platform(INSTRUCTURE)
                .domain(request.getServerName())
                .privacyLevel(Lti13Config.PrivacyLevel.PUBLIC)
                .settings(canvas13Settings)
                .createCanvas13Extension());
        Map<String, String> customFields = new HashMap<>();
        customFields.put("canvas_course_id", "$Canvas.course.id");
        customFields.put("canvas_account_id", "$Canvas.account.id");
        customFields.put("canvas_root_account_global_id", "$Canvas.root_account.global_id");
        customFields.put("canvas_membership_roles", "$Canvas.membership.roles");
        customFields.put("canvas_api_base_url", "$Canvas.api.baseUrl");
        customFields.put("com_instructure_brand_config_json_url", "$com.instructure.brandConfigJSON.url");
        customFields.put("allowed_roles", "Account admin,Learning Technologist,Unit Admin,Local Canvas Coordinator,Implementation Coordinator,Data Administrator");
        String client = "oxeval-cm";
        return new Lti13ConfigBuilder()
                .title("Course Management")
                .description("Allows the creation and rollover of Canvas courses.")
                .oidcInitiaionUrl(urlPrefix + "/lti/login_initiation/"+ client)
                // This should be the frontend URL.
                .targetLinkUri("https://localhost:3000/")
                .extensions(extensions)
                .publicJwk(jwkSet.getKeyByKeyId(jwtId).toPublicJWK().toJSONObject())
                .customFields(customFields)
                .createLti13Config();
    }


}
