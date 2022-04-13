package io.jans.as.model.common.converter;

import com.fasterxml.jackson.databind.util.StdConverter;
import io.jans.as.model.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class to facilitate two-step deserialization.
 */
public class ListConverter extends StdConverter<Object, List<String>> {

    /**
     * Converts a value to a List of Strings. Conversion is attempted only
     * if parameter obj is already a String or a List. In case of String, a
     * whitespace is assumed as elements separator
     *
     * @param obj Input object
     * @return A list of strings, empty if obj is null or does not have the expected type
     */
    public List<String> convert(Object obj) {

        if (obj != null) {
            if (List.class.isAssignableFrom(obj.getClass())) {
                // json data already looks like a list...
                Stream<String> stream = ((List) obj).stream()
                        .filter(Objects::nonNull).map(Object::toString);
                return stream.collect(Collectors.toList());

            } else if (String.class.equals(obj.getClass())) {
                return StringUtils.spaceSeparatedToList(obj.toString());
            }
        }
        return new ArrayList<>();

    }

}
