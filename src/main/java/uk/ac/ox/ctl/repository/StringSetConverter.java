package uk.ac.ox.ctl.repository;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This allows us to store a small set of strings into a single field in a DB.
 * This means we don't have to do an N+1 lookup when loading objects.
 */
@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
    private final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<String> stringList) {
        if (stringList == null) {
            return "";
        }
        // TODO This isn't ideal, really we should have a generic solution here that supports any characters.
        if (stringList.stream().anyMatch(string -> string.contains(SEPARATOR))) {
            throw new IllegalArgumentException("None of the items in the set can contain: "+ SEPARATOR);
        }
        return String.join(SEPARATOR, stringList);
    }

    @Override
    public Set<String> convertToEntityAttribute(String packed) {
        if (packed == null || packed.isEmpty()) {
            return Collections.emptySet();
        } else {
            return new HashSet<>(List.of(packed.split(",")));
        }
    }
}