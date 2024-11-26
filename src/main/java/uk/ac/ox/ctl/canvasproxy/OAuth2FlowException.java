package uk.ac.ox.ctl.canvasproxy;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
/**
 * Exception that is used to display a message to user explaining that the OAuth flow failed.
 */
public class OAuth2FlowException extends AuthenticationException {
	public OAuth2FlowException(String msg) {
		super(msg);
	}
}
