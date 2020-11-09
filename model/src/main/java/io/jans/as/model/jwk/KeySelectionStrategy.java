package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jans.orm.annotation.AttributeEnum;

import java.util.Collections;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */
public enum KeySelectionStrategy implements AttributeEnum {
    OLDER,
    NEWER,
    FIRST;

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public Enum<? extends AttributeEnum> resolveByValue(String s) {
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonIgnore
    public JSONWebKey select(List<JSONWebKey> list) {
        if (list == null || list.isEmpty())
            return null;

        if (this == FIRST)
            return list.iterator().next();

        if (this == OLDER)
            return Collections.min(list);

        if (this == NEWER)
            return Collections.max(list);

        return null;
    }
}
