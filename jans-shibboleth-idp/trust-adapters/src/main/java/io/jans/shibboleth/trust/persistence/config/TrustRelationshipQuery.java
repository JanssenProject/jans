package io.jans.shibboleth.trust.persistence.config;

/**
 * Query parameters for listing trust relationships (D14): optional substring filters on the summary
 * columns plus 1-based paging. An immutable value carried into the repository's query projection; the
 * repository translates it into a jans-orm {@code Filter} + paged search.
 */
public final class TrustRelationshipQuery {

    private final String displayNameContains;
    private final String descriptionContains;
    private final int page;
    private final int size;

    public TrustRelationshipQuery(String displayNameContains, String descriptionContains, int page, int size) {

        this.displayNameContains = displayNameContains;
        this.descriptionContains = descriptionContains;
        this.page = page;
        this.size = size;
    }

    public String getDisplayNameContains() {

        return displayNameContains;
    }

    public String getDescriptionContains() {

        return descriptionContains;
    }

    /** 1-based page number; the first page is 1 (D14). */
    public int getPage() {

        return page;
    }

    public int getSize() {

        return size;
    }

    public boolean hasDisplayNameFilter() {

        return displayNameContains != null && !displayNameContains.isBlank();
    }

    public boolean hasDescriptionFilter() {

        return descriptionContains != null && !descriptionContains.isBlank();
    }
}
