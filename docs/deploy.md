# Deploying Tool Support

## Test Docker Setup

If you would just like to get a copy of tool-support up and working to test with there is a docker setup that allows
you to run it without having to build it.

### Prerequisites

To do this you need to have these tools installed:

- docker - https://docker.com `docker-compose` is the key part.
- mkcert - https://github.com/FiloSottile/mkcert to generate self signed certificates.

### Setup

Before starting up the docker container you need to create a certificate file to enable HTTPS.

```bash
mkcert -pkcs12 -p12-file config/keystore.p12 localhost
```

It is also adviseable to set a password for the admin user so that it will remain the same between restarts. Change `my-password` to something more secure.

```bash
cat <<EOF > config/application.properties
spring.security.user.name=admin
spring.security.user.roles=admin
spring.security.user.password=my-password
EOF
```

### Run

To start up the containers run:

```bash
docker-compose up
```

This should start up the mysql database and the application server listening on https://localhost:8443

This will use a prebuild docker image for the application server that is pulled from GitHub.

### Configure Tools

Not you can configure a tool through the API. There is support for automatically configuring tools with https://github.com/oxctl/lti-auto-configuration

There's the [Example LTI 1.3 Tool](https://github.com/oxctl/lti-13-example) which can be used to test that you have the service running correctly.

