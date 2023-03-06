package uk.ac.ox.ctl;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class ClientAuthenticationMethodJsonConverter {
    public static class Deserialize extends StdConverter<String, ClientAuthenticationMethod> {
        @Override
        public ClientAuthenticationMethod convert(String value) {
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equalsIgnoreCase(value)
                    || ClientAuthenticationMethod.BASIC.getValue().equalsIgnoreCase(value)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            }
            if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equalsIgnoreCase(value)
                    || ClientAuthenticationMethod.POST.getValue().equalsIgnoreCase(value)) {
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
            return clientAuthenticationMethod != null ? clientAuthenticationMethod.toString() : null;
        }
    }
}


