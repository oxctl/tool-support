package uk.ac.ox.ctl.canvasproxy;

/**
 * This interface is used to lookup details based on the audience of a JWT.
 * The audience normally comes from the JWT that was created for a LTI launch.
 */
public interface AudienceConfigResolver {

    public byte[] findHmacSecret(String audience);

    public String findIssuer(String audience);

    public String findProxyRegistration(String audience);
}
