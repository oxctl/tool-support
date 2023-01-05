package uk.ac.ox.ctl.ltiauth.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties("lti.nrps")
public class AllowedRoles {

    private Map<String, Roles> client;

    public Map<String, Roles> getClient() {
        return client;
    }

    public void setClient(Map<String, Roles> client) {
        this.client = client;
    }

    public static class Roles {

        private Set<String> roles;

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles;
        }
    }

}
