package uk.ac.ox.ctl.repository;


import org.springframework.security.oauth2.core.AuthorizationGrantType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class AuthorizationGrantTypeConverter implements AttributeConverter<AuthorizationGrantType, String> {

    @Override
    public String convertToDatabaseColumn(AuthorizationGrantType grantType) {
        if (grantType == null) {
            return null;
        }
        return grantType.getValue();
    }

    @Override
    public AuthorizationGrantType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        // As these are immutable we could optimise this to re-use the same instance.
        return new AuthorizationGrantType(value);
    }

}