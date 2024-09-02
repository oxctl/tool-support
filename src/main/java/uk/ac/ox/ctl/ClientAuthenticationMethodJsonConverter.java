package uk.ac.ox.ctl;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class ClientAuthenticationMethodJsonConverter {
    public static class Deserialize extends StdConverter<String, ClientAuthenticationMethod> {
        @Override
        public ClientAuthenticationMethod convert(String value) {
            // In the past Spring didn't prefix the values with "client_secret" so the constants
            // "basic" and "post" are there to support the old values.
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equalsIgnoreCase(value)
                    || "basic".equalsIgnoreCase(value)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            }
            if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equalsIgnoreCase(value)
                    || "post".equalsIgnoreCase(value)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_POST;
            }
            if (ClientAuthenticationMethod.NONE.getValue().equalsIgnoreCase(value)) {
                return ClientAuthenticationMethod.NONE;
            }
            return null;
        }
    }

    public static class Serialize extends StdConverter<ClientAuthenticationMethod, String>{
        @Override
        public String convert(ClientAuthenticationMethod clientAuthenticationMethod) {
            return clientAuthenticationMethod != null ? clientAuthenticationMethod.getValue() : null;
        }
    }
}


