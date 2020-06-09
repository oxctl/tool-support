package uk.ac.ox.ctl.canvasproxy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

/**
 * This holds the tokens for a user. Each token can be null, but if not null then they have to be
 * valid.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class PrincipalTokens {

  // The user to who the tokens belong
  @Id
  private String principal;

  @Embedded @Valid private AccessToken accessToken;

  @Embedded @Valid private RefreshToken refreshToken;

  public PrincipalTokens(String principal, OAuth2AuthorizedClient oauth2AuthorizedClient) {
    this.principal = principal;
    this.accessToken = new AccessToken(oauth2AuthorizedClient.getAccessToken());
    if (oauth2AuthorizedClient.getRefreshToken() != null) {
      this.refreshToken = new RefreshToken(oauth2AuthorizedClient.getRefreshToken());
    }
  }

  public OAuth2AuthorizedClient toOAuth2AuthorizedClient(
      ClientRegistration client, String principal) {
    return new OAuth2AuthorizedClient(
        client,
        principal,
        getAccessToken().toOAuth2AccessToken(),
        getRefreshToken() == null ? null : getRefreshToken().toOAuth2RefreshToken());
  }

  /**
   * We pack the scopes into a string as they are space seperated according to the spec so we can
   * re-use that fact.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6749#section-3.3">RFC-6749 CourseSection 3.3</a>
   */
  @Data
  @Embeddable
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccessToken {

    @Column(name = "access_token_value")
    @NotNull
    @ToString.Exclude
    private String tokenValue;

    @Column(name = "access_issued_at")
    @NotNull
    private Instant issuedAt;

    @Column(name = "access_expires_at")
    @NotNull
    private Instant expiresAt;

    @Column(name = "access_scopes")
    @NotNull
    private String scopes;

    public AccessToken(OAuth2AccessToken oauth2Token) {
      tokenValue = oauth2Token.getTokenValue();
      issuedAt = oauth2Token.getIssuedAt();
      expiresAt = oauth2Token.getExpiresAt();
      scopes = String.join(" ", oauth2Token.getScopes());
    }

    public OAuth2AccessToken toOAuth2AccessToken() {
      Set<String> scopes =
          (this.scopes != null && !this.scopes.isEmpty())
              ? new HashSet<>(Arrays.asList(this.scopes.split(" ")))
              : Collections.emptySet();
      OAuth2AccessToken oauth2token =
          new OAuth2AccessToken(BEARER, tokenValue, issuedAt, expiresAt, scopes);
      return oauth2token;
    }
  }

  @Data
  @Embeddable
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RefreshToken {

    @Column(name = "refresh_token_value")
    @NotNull
    @ToString.Exclude
    private String tokenValue;

    @Column(name = "refresh_issues_at")
    @NotNull
    private Instant issuedAt;

    public RefreshToken(OAuth2RefreshToken oauth2Token) {
      tokenValue = oauth2Token.getTokenValue();
      issuedAt = oauth2Token.getIssuedAt();
    }

    public OAuth2RefreshToken toOAuth2RefreshToken() {
      OAuth2RefreshToken oauth2token = new OAuth2RefreshToken(tokenValue, issuedAt);
      return oauth2token;
    }
  }
}
