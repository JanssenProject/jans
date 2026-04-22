package io.jans.shibboleth.model.config.profiles.common;

import java.util.List;
import java.util.stream.Collectors;

public class NameIdentifiers {

    private final List<String> nameIds;

    public NameIdentifiers(List<String> nameIds) {

        this.nameIds = nameIds != null ? List.copyOf(nameIds) : List.of() ;
    }

    public static NameIdentifiers empty() {

        return new NameIdentifiers(null);
    }

    public static NameIdentifiers of(List<String> nameIds) {

        List<String> newNameIds = nameIds.stream()
            .map( nameId -> { return nameId.trim(); } )
            .filter( nameId -> { return !nameId.isBlank();} )
            .collect(Collectors.toList());
        
        return new NameIdentifiers(newNameIds);
    }

    public boolean hasNone() {

        return nameIds.isEmpty();
    }

    public boolean hasSome() {

        return ! nameIds.isEmpty();
    }
}