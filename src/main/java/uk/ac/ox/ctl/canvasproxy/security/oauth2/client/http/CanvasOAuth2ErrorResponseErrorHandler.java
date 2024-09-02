/*
 * Copyright 2002-2021 the original author or authors.
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

package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.http;

import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * A {@link ResponseErrorHandler} that handles an {@link OAuth2Error OAuth 2.0 Error}.
 *
 * @author Joe Grandja
 * @since 5.1
 * @see ResponseErrorHandler
 * @see OAuth2Error
 */
public class CanvasOAuth2ErrorResponseErrorHandler implements ResponseErrorHandler {

	private HttpMessageConverter<OAuth2Error> oauth2ErrorConverter = new OAuth2ErrorHttpMessageConverter();

	private final ResponseErrorHandler defaultErrorHandler = new DefaultResponseErrorHandler();

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return this.defaultErrorHandler.hasError(response) || hasErrorCanvas(response);
	}

	/**
	 * Canvas for a time used a 302 redirect to indicate a problem with retrieving the token.
	 * As of 2023-04-15 it should no longer be needed, and we should get a 400 error, however
	 * we might need something to extra the error still.
	 *
	 * <a href="https://community.canvaslms.com/t5/Canvas-Change-Log/Canvas-Platform-Breaking-Changes/ta-p/262015">...</a>
	 *
	 */
	private static boolean hasErrorCanvas(ClientHttpResponse response) throws IOException {
		int rawStatusCode = response.getStatusCode().value();
		HttpStatus statusCode = HttpStatus.resolve(rawStatusCode);
		return HttpStatus.FOUND.equals(statusCode);
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (HttpStatus.FOUND.value() == response.getStatusCode().value()) {
			handleErrorCanvas(response);
		}
		if (HttpStatus.BAD_REQUEST.value() != response.getStatusCode().value() &&
				HttpStatus.UNAUTHORIZED.value() != response.getStatusCode().value()) {
			this.defaultErrorHandler.handleError(response);
		}
		// See https://tools.ietf.org/html/rfc6750#section-3
		// The body generally seems to have a better error.
		OAuth2Error oauth2Error = this.oauth2ErrorConverter.read(OAuth2Error.class, response);
		if (oauth2Error == null) {
			// A Bearer Token Error may be in the WWW-Authenticate response header
			oauth2Error = this.readErrorFromWwwAuthenticate(response.getHeaders());
		}
		throw new OAuth2AuthorizationException(oauth2Error);
	}

	private void handleErrorCanvas(ClientHttpResponse response) {
		URI location = response.getHeaders().getLocation();
		OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, null, null);
		if (location != null) {
			UriComponents components = UriComponentsBuilder.fromUri(location).build();
			MultiValueMap<String, String> params = components.getQueryParams();
			String error = params.getFirst("error");
			String errorDescription = params.getFirst("error_description");
			oAuth2Error = new OAuth2Error(error, errorDescription, null);
		}
		throw new OAuth2AuthorizationException(oAuth2Error);
	}

	private OAuth2Error readErrorFromWwwAuthenticate(HttpHeaders headers) {
		String wwwAuthenticateHeader = headers.getFirst(HttpHeaders.WWW_AUTHENTICATE);
		if (!StringUtils.hasText(wwwAuthenticateHeader)) {
			return null;
		}
		BearerTokenError bearerTokenError = getBearerToken(wwwAuthenticateHeader);
		if (bearerTokenError == null) {
			return new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, null, null);
		}
		String errorCode = (bearerTokenError.getCode() != null) ? bearerTokenError.getCode()
				: OAuth2ErrorCodes.SERVER_ERROR;
		String errorDescription = bearerTokenError.getDescription();
		String errorUri = (bearerTokenError.getURI() != null) ? bearerTokenError.getURI().toString() : null;
		return new OAuth2Error(errorCode, errorDescription, errorUri);
	}

	private BearerTokenError getBearerToken(String wwwAuthenticateHeader) {
		try {
			return BearerTokenError.parse(wwwAuthenticateHeader);
		} catch (Exception ex) {
			return null;
		}
	}


	/**
	 * Sets the {@link HttpMessageConverter} for an OAuth 2.0 Error.
	 * @param oauth2ErrorConverter A {@link HttpMessageConverter} for an
	 * {@link OAuth2Error OAuth 2.0 Error}.
	 * @since 5.7
	 */
	public final void setErrorConverter(HttpMessageConverter<OAuth2Error> oauth2ErrorConverter) {
		Assert.notNull(oauth2ErrorConverter, "oauth2ErrorConverter cannot be null");
		this.oauth2ErrorConverter = oauth2ErrorConverter;
	}

}
