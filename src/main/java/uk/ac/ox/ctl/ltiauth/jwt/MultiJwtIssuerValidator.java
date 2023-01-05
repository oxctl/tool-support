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
package uk.ac.ox.ctl.ltiauth.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.Assert;

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

	private final String[] issuers;

	/**
	 * Constructs a {@link MultiJwtIssuerValidator} using the provided parameters
	 *
	 * @param issuers - The issuer that each {@link Jwt} should have.
	 */
	public MultiJwtIssuerValidator(String... issuers) {
		Assert.notNull(issuers, "issuers cannot be null");
		Assert.notEmpty(issuers, "issuers cannot be empty");
		this.issuers = issuers;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Assert.notNull(token, "token cannot be null");

		String tokenIssuer = token.getClaimAsString(JwtClaimNames.ISS);
		for (String issuer: issuers) {
			if (issuer.equals(tokenIssuer)) {
				return OAuth2TokenValidatorResult.success();
			}
		}
		return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
	}
}
