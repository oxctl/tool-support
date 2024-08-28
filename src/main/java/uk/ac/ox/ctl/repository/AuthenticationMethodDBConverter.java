package uk.ac.ox.ctl.repository;


import org.springframework.security.oauth2.core.AuthenticationMethod;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuthenticationMethodDBConverter implements AttributeConverter<AuthenticationMethod, String> {

    @Override
    public String convertToDatabaseColumn(AuthenticationMethod authMethod) {
        if (authMethod == null) {
            return null;
        }
        return authMethod.getValue();
    }

    @Override
    public AuthenticationMethod convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        // As these are immutable we could optimise this to re-use the same instance.
        return new AuthenticationMethod(value);

    }

}