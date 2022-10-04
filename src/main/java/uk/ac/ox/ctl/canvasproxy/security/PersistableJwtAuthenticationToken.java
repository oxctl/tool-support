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

package uk.ac.ox.ctl.canvasproxy.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;

import java.util.Collection;
import java.util.Map;

/**
 * An implementation of an {@link AbstractOAuth2TokenAuthenticationToken} representing a
 * {@link Jwt} {@code Authentication}.
 * 
 * This was copied from Spring and the {@code Transient} annotation was removed so we can store it in the session.
 *
 * @author Joe Grandja
 * @author Matthew Buckett
 * @since 5.1
 * @see AbstractOAuth2TokenAuthenticationToken
 * @see Jwt
 */
public class PersistableJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

	private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

	private final String name;

	/**
	 * Constructs a {@code JwtAuthenticationToken} using the provided parameters.
	 * @param jwt the JWT
	 */
	public PersistableJwtAuthenticationToken(Jwt jwt) {
		super(jwt);
		this.name = jwt.getSubject();
	}

	/**
	 * Constructs a {@code JwtAuthenticationToken} using the provided parameters.
	 * @param jwt the JWT
	 * @param authorities the authorities assigned to the JWT
	 */
	public PersistableJwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
		super(jwt, authorities);
		this.setAuthenticated(true);
		this.name = jwt.getSubject();
	}

	/**
	 * Constructs a {@code JwtAuthenticationToken} using the provided parameters.
	 * @param jwt the JWT
	 * @param authorities the authorities assigned to the JWT
	 * @param name the principal name
	 */
	public PersistableJwtAuthenticationToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities, String name) {
		super(jwt, authorities);
		this.setAuthenticated(true);
		this.name = name;
	}

	@Override
	public Map<String, Object> getTokenAttributes() {
		return this.getToken().getClaims();
	}

	/**
	 * The principal name which is, by default, the {@link Jwt}'s subject
	 */
	@Override
	public String getName() {
		return this.name;
	}

}
