package io.jans.casa.core.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;

import java.util.Optional;

@DataEntry
@ObjectClass("jansScope")
public class Scope extends InumEntry {

    @AttributeName
    private String description;

    @AttributeName(name = "jansId")
    private String id;

    public String getDescription() {
        return description;
    }

    public String getId()
    {
        return id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = o != null && o instanceof Scope;
        if (equal) {
            String otherId = Scope.class.cast(o).getId();
            equal = Optional.ofNullable(getId()).map(name -> name.equals(otherId)).orElse(otherId == null);
        }
        return equal;
    }

}
