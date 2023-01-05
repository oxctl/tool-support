package uk.ac.ox.ctl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;

import java.security.Principal;
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
  public String canvasCommonCss(Principal principal) {
    String canvasCss = null;
    if (principal instanceof AbstractOAuth2TokenAuthenticationToken) {
      AbstractOAuth2TokenAuthenticationToken token = (AbstractOAuth2TokenAuthenticationToken) principal;
      if(token.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom") != null) {
        canvasCss = (String) ((Map) token.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("canvas_css_common");
      }
    }
    if (canvasCss == null) {
      canvasCss = defaultCommonCss;
    }
    return canvasCss;
  }

  // TODO Not sure this will work...
  @ModelAttribute("canvasBrandCss")
  public String canvasBrandCss(Principal principal) {
    String canvasJson = null;
    if (principal instanceof AbstractOAuth2TokenAuthenticationToken) {
      AbstractOAuth2TokenAuthenticationToken token = (AbstractOAuth2TokenAuthenticationToken) principal;
      if (token.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom") != null) {
        canvasJson = (String) ((Map) token.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("com_instructure_brand_config_json_url");
      }
    }
    if (canvasJson == null) {
      canvasJson = defaultBrandJson;
    }
    // There isn't a CSS brand LTI variable so we just get the path to the .json file and assume there is a .css version
    // deployed at the same location.
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
