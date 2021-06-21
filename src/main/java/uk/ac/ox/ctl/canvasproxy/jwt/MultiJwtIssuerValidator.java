/*
 * Copyright 2002-2018 the original author or authors.
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
package uk.ac.ox.ctl.canvasproxy.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.Assert;

import java.util.List;
import java.util.function.Function;

/**
 * Validates the "iss" claim in a {@link Jwt}, that is matches one of the configured values
 *
 * @author Matthew Buckett
 */
public final class MultiJwtIssuerValidator implements OAuth2TokenValidator<Jwt> {
	private static OAuth2Error INVALID_ISSUER =
			new OAuth2Error(
					OAuth2ErrorCodes.INVALID_REQUEST,
					"This iss claim is not equal to the configured issuer",
					"https://tools.ietf.org/html/rfc6750#section-3.1");

	private final Function<Jwt, List<String>> lookupIssuers;

	/**
	 * Constructs a {@link MultiJwtIssuerValidator} using the provided parameters
	 *
	 * @param lookupIssuers A function to lookup the valid issuers to use for a JWT.
	 */
	public MultiJwtIssuerValidator(Function<Jwt, List<String>> lookupIssuers) {
		this.lookupIssuers = lookupIssuers;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Assert.notNull(token, "token cannot be null");

		String tokenIssuer = token.getClaimAsString(JwtClaimNames.ISS);
		for (String issuer: lookupIssuers.apply(token)) {
			if (issuer.equals(tokenIssuer)) {
				return OAuth2TokenValidatorResult.success();
			}
		}
		return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
	}
}
