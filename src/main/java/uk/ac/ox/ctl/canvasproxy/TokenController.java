package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.util.HashMap;
import java.util.Map;

@Controller()
@RequestMapping("/tokens")
public class TokenController {

    private Logger log = LoggerFactory.getLogger(TokenController.class);

    @Value("${canvas.common.css}")
    private String defaultCommonCss;

    @Value("${canvas.brand.json}")
    private String defaultBrandJson;

    @Value("${spring.application.name}")
    private String applicationName;

    @ModelAttribute("canvasCommonCss")
    public String canvasCommonCss(JwtAuthenticationToken principal) {
        String canvasCss = null;
        if (principal != null && principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom") != null) {
            canvasCss = (String) ((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("canvas_css_common");
        }
        if (canvasCss == null) {
            canvasCss = defaultCommonCss;
        }
        return canvasCss;
    }

    @ModelAttribute("canvasBrandCss")
    public String canvasBrandCss(JwtAuthenticationToken principal) {
        String canvasJson = null;
        if (principal != null && principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom") != null) {
            canvasJson = (String) ((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("com_instructure_brand_config_json_url");

            if (canvasJson == null) {
                canvasJson = defaultBrandJson;
            }
        }
        // This is a fudge as at the moment a CSS version exists alongside the JS version.
        String canvasCss = null;
        if (canvasJson != null) {
            if (canvasJson.endsWith(".json")) {
                canvasCss = canvasJson.substring(0, canvasJson.length() - ".json".length()) + ".css";
            }
        }
        return canvasCss;
    }


    @RequestMapping("/check")
    public ModelAndView token(JwtAuthenticationToken authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client) {
        Map<String, Object> model = new HashMap<>();
        model.put("applicationName", client.getClientRegistration().getClientName());
        model.put("target", authenticationToken.getToken().getClaim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri"));
        model.put("error", "None");
        model.put("message", "None");
        return new ModelAndView("login-done", model);
    }

}
