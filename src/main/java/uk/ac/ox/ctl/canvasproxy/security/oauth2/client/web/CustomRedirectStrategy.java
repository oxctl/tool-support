/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;

/**
 * Because we are making XHR requests rather than document requests we need to send a different response, rather
 * than directly redirecting the client. So we set a custom error code so the client knows to open browser window
 * where the user can authorize the application.
 */
public class CustomRedirectStrategy implements RedirectStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Redirects the response to the supplied URL.
	 * <p>
	 * If <tt>contextRelative</tt> is set, the redirect value will be the value after the
	 * request context path. Note that this will result in the loss of protocol
	 * information (HTTP or HTTPS), so will cause problems if a redirect is being
	 * performed to change to HTTPS, for example.
	 */
	public void sendRedirect(HttpServletRequest request, HttpServletResponse response,
			String url) throws IOException {
		// Ideally the client should use this URL to get the user to give consent.
		String redirectUrl = "/tokens/check";
		redirectUrl = response.encodeRedirectURL(redirectUrl);

		if (logger.isDebugEnabled()) {
			logger.debug("Redirecting to '" + redirectUrl + "'");
		}
		response.addHeader("Location", redirectUrl);
		response.sendError(HttpStatus.FORBIDDEN.value(), "No client found");
	}
}
