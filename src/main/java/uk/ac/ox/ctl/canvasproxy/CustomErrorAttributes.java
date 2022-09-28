package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * This is used so that when we are displaying an error page if it's because the user didn't
 * grant us access to their account we give them a second chance by putting the URL to go back and
 * have another go in the page.
 */
public class CustomErrorAttributes extends DefaultErrorAttributes {

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions errorAttributeOptions) {
    Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, errorAttributeOptions);
    Throwable error = getError(webRequest);
    if (error instanceof OAuth2AccessDeniedException) {
      String reloginUrl = ((OAuth2AccessDeniedException) error).getReloginUrl();
      if (reloginUrl != null) {
        errorAttributes.put("reloginUrl", reloginUrl);
      }
    }
    return errorAttributes;
  }
}
