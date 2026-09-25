package io.jans.shibboleth.trust.config.profile.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public List<String> getNameIdentifiers() {

        return nameIds;
    }

    public boolean hasNone() {

        return nameIds.isEmpty();
    }

    public boolean hasSome() {

        return ! nameIds.isEmpty();
    }

    public NameIdentifiers add(String ... nameIdsToAdd) {

        if (nameIdsToAdd == null || nameIdsToAdd.length == 0) {

            return this;
        }

        Set<String> merged  = new LinkedHashSet<>(this.nameIds);
        for (String n : nameIdsToAdd) {

            String cleaned  = n == null ? null : n.trim();
            if ( cleaned != null && !cleaned.isBlank() ) {
                merged.add(cleaned);
            }
        }

        return new NameIdentifiers(new ArrayList<>(merged));
    }

    public NameIdentifiers addAll(Collection<String> nameIdsToAdd) {

        if (nameIdsToAdd == null || nameIdsToAdd.isEmpty()) {

            return this;
        }

        return add(nameIdsToAdd.toArray(new String[0]));
    }

    public NameIdentifiers remove(String ... nameIdsToRemove) {

        if(nameIdsToRemove == null || nameIdsToRemove.length == 0 || nameIds.isEmpty()) {

            return this;
        }

        Set<String> toRemove = Arrays.stream(nameIdsToRemove)
            .map(s -> s == null ? null : s.trim())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.toSet());
        
        if (toRemove.isEmpty()) {

            return this;
        }

        List<String> remaining = this.nameIds.stream()
            .filter( n -> !toRemove.contains(n))
            .collect(Collectors.toList());
        
        return new NameIdentifiers(remaining);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        
        if (o == null || getClass() != o.getClass()) return false;

        NameIdentifiers that = (NameIdentifiers) o;

        return nameIds.equals(that.nameIds);
    }

    @Override
    public int hashCode() {

        return nameIds.hashCode();
    }
}