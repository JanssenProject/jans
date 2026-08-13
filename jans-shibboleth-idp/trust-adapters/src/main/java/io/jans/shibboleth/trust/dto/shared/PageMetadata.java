package io.jans.shibboleth.trust.dto.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Pagination metadata for a page of results. Page numbers are 1-based (the first page is 1).
 */
public class PageMetadata {

    @JsonProperty("size")
    private final int size;

    @JsonProperty("number")
    private final int number;

    @JsonProperty("total_elements")
    private final long totalElements;

    @JsonProperty("total_pages")
    private final int totalPages;

    @JsonProperty("number_of_elements")
    private final int numberOfElements;

    public PageMetadata(int size, int number, long totalElements, int totalPages, int numberOfElements) {

        this.size = size;
        this.number = number;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.numberOfElements = numberOfElements;
    }

    public int getSize() {

        return size;
    }

    public int getNumber() {

        return number;
    }

    public long getTotalElements() {

        return totalElements;
    }

    public int getTotalPages() {

        return totalPages;
    }

    public int getNumberOfElements() {

        return numberOfElements;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageMetadata that = (PageMetadata) o;
        return size == that.size
            && number == that.number
            && totalElements == that.totalElements
            && totalPages == that.totalPages
            && numberOfElements == that.numberOfElements;
    }

    @Override
    public int hashCode() {

        return Objects.hash(size, number, totalElements, totalPages, numberOfElements);
    }

    @Override
    public String toString() {

        return "PageMetadata{size=" + size + ", number=" + number + ", totalElements=" + totalElements
            + ", totalPages=" + totalPages + ", numberOfElements=" + numberOfElements + '}';
    }
}
