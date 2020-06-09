package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is the access token for a client. This is useful so that a client doesn't get a token that is
 * already expired.
 */
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisteredOAuth2AccessToken {

    /**
     * Sets the client registration identifier.
     *
     * @return the client registration identifier
     */
    @AliasFor("value")
    String registrationId() default "";

    /**
     * The default attribute for this annotation. This is an alias for {@link #registrationId()}. For
     * example, {@code @RegisteredOAuth2AuthorizedClient("login-client")} is equivalent to
     * {@code @RegisteredOAuth2AuthorizedClient(registrationId="login-client")}.
     *
     * @return the client registration identifier
     */
    @AliasFor("registrationId")
    String value() default "";

    /**
     * Set if this OAuth2AccessToken is required.
     * @return if the parameter is required.
     */
    boolean required() default true;

}
