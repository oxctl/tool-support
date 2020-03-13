# Canvas Proxy

This is a simple webapp that just takes a HTTP request and proxies it through to Canvas. This allows a frontend webapp to be able to make XHR requests to Canvas. It should support multiple developer keys so that it can support multiple applications using it. It needs to request tokens for users, then maybe hand them back to the client (HTML/JS) so that they can be stored in local storage. That way we don't need any persistence server side, although we don't want to pass 2 tokens so we should probably re-wrap the token from Canvas with our own so that we don't rely on the internal structure of the token we get back from canvas.

To start with we should just persist the tokens in a DB.

## Status

There is a small proxy controller that sends requests on the Canvas with a hard coded token. Headers get passed through and responses get passed back. At the moment there seems to be a bug with HTML responses. There's no authentication.


Lifetime on tokens is currently pretty low. JS OAuth library?

We are going to want to support multiple tenants and so will want to be able to map from a JWT to a Canvas endpoint to use. We also need to check that the audience on the JWT maps to the client ID that should be used on the OAuth token.

Request comes in with a JWT and a "aud" we lookup the audience to work out the registration to use. This is similar to how we map from a LTIPrincipal to a registration.

## Deployment Configuration

### AWS Elastic Beanstalk

This application needs a database to store the OAuth tokens it gets granted. Recovery for this database isn't critical at the moment as if wee lost it then everyone would need to confirm that they wanted to grant access to their account again.

#### Environmental Variables

- HOSTNAME - The hostname that the server is running on, used to get TLS/config files.
- RDS_HOSTNAME - The MySQL hostname to connect to for the database.
- RDS_PORT - The MySQL port to use in the connection to thee database.
- RDS_DB_NAME - The name of the MySQL database to use.
- RDS_USERNAME - The username to use to connect to the MySQL database.
- RDS_PASSWORD - The password to use to connect to the MySQL database.

#### Client Configuration

For each client that can use the tool there needs to be an entry in the client configuration file. This file then needs to be updated to S3. To download the current clients:

    aws s3 cp s3://elasticbeanstalk-eu-west-1-075499702012/files/proxy.canvas.ox.ac.uk-client.properties .

then you can edit the file and upload it to S3 again:

    aws s3 cp proxy.canvas.ox.ac.uk-client.properties s3://elasticbeanstalk-eu-west-1-075499702012/files/

The client will probably also want to have it's HTTPS endpoint added to the CORS configuration in the same file. To add a new client first you need to decide on the "Client Registration ID" which is a name used by the spring configuration. This will form part of all the properties. For each client you need a section in the configuration like:

    # Replace [clientRegId] and [instance] with values for your tool
    # This first section can be shared if there are multiple tools
    spring.security.oauth2.client.provider.[instance].authorization-uri=https://[instance].instructure.com/login/oauth2/auth
    spring.security.oauth2.client.provider.[instance].token-uri=https://[instance].instructure.com/login/oauth2/token
    
    # This is the details of the tool
    # User friendly name of the registration.
    spring.security.oauth2.client.registration.[clientRegId].client-name=
    # The client ID from Canvas
    spring.security.oauth2.client.registration.[clientRegId].client-id=
    # The client secret from Canvas
    spring.security.oauth2.client.registration.[clientRegId].client-secret=
    spring.security.oauth2.client.registration.[clientRegId].redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
    spring.security.oauth2.client.registration.[clientRegId].authorization-grant-type=authorization_code
    spring.security.oauth2.client.registration.[clientRegId].client-authentication-method=post
    spring.security.oauth2.client.registration.[clientRegId].provider=[instance]
    
Then you will also need a mapping from the LTI Key to the Developer Key

    # Set this equal to the client ID of the LTI tool.
    proxy.mapping.[clientRegId]=

#### HTTPS

A public private keypair needs to be generated for TLS. There are configurations for TLS in `proxy.canvas.ox.ac.uk.cfg` to create a new private key and CSR use:

    openssl req -nodes -new -keyout proxy.canvas.ox.ac.uk-key.pem -out proxy.canvas.ox.ac.uk.csr -config proxy.canvas.ox.ac.uk.cfg -batch -verbose

This CSR can then be used to request a certificate from the certificate service: https://wiki.it.ox.ac.uk/itss/CertificateService 
Once a certificate is issued they should be uploaded to the S3 bucket:

    aws s3 cp proxy.canvas.ox.ac.uk-chain.crt  s3://elasticbeanstalk-eu-west-1-075499702012/certificates/
    aws s3 cp proxy.canvas.ox.ac.uk-key.pem s3://elasticbeanstalk-eu-west-1-075499702012/certificates/

## TODO

### Refresh/Access tokens

Handling refresh/access tokens and updating them in the DB, at the moment we have to re-ask every 1 hour.

### Error Handling

If the user removes their token then we get a 401 back from Canvas with a body of:
{"errors":[{"message":"Invalid access token."}]}
We need to be careful with this as when our JWT is invalid we will also get 401, one difference is that the error proxied from canvas is the header:

    WWW-Authenticate: Bearer realm="canvas-lms"

but when it's from an missing JWT it's:

     WWW-Authenticate: Bearer realm="proxy"
    
and if it's an invalid JWT it's something along the lines of:

    WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Signed JWT rejected: Another algorithm expected, or no matching key(s) found", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"

### Request Body

Need to test the handling of request bodies and if they get correctly mapped through the proxy to Canvas.

### Frontend from config

We need to pull the frontend application from configuration so that we can support multiple frontends.

