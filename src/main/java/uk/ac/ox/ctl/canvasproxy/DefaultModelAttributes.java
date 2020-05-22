package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/**
 * As we don't really have any pages that the user sees, these attributes are mainly used for the error
 * pages.
 */
@ControllerAdvice
public class DefaultModelAttributes {

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
    }
    if (canvasJson == null) {
      canvasJson = defaultBrandJson;
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

  @ModelAttribute("applicationName")
  public String applicationName() {
    return applicationName;
  }
}
