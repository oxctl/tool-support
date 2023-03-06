package uk.ac.ox.ctl.repository;


import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ClientAuthenticationMethodDBConverter implements AttributeConverter<ClientAuthenticationMethod, String> {

    @Override
    public String convertToDatabaseColumn(ClientAuthenticationMethod authMethod) {
        if (authMethod == null) {
            return null;
        }
        return authMethod.getValue();
    }

    @Override
    public ClientAuthenticationMethod convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        // As these are immutable we could optimise this to re-use the same instance.
        return new ClientAuthenticationMethod(value);

    }

}