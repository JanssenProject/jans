package io.jans.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Where a failure occurred, as segments read outermost-first ({@code metadataSource.validUntil}).
 *
 * <p>A value object cannot know its own location. {@code DisplayName} has no way to tell whether it
 * sits at {@code displayName} on a trust relationship, or nested under a metadata source, or is
 * being validated standalone. Location is therefore contributed by whichever caller composes the
 * value — one segment at a time, as the failure travels outward. {@link Result#at(String)} is how a
 * caller contributes its segment.
 *
 * <p>{@link #empty()} is the absence of a location, not a null and not an absent optional: an error
 * raised by a single-field value object names its owning type and needs no path at all.
 */
public final class FieldPath {

    private static final FieldPath EMPTY = new FieldPath(Collections.emptyList());

    private final List<String> segments;

    private FieldPath(List<String> segments) {

        this.segments = segments;
    }

    public static FieldPath empty() {

        return EMPTY;
    }

    public static FieldPath of(String... segments) {

        List<String> collected = new ArrayList<>(segments.length);
        for (String segment : segments) {

            collected.add(requireSegment(segment));
        }

        return collected.isEmpty() ? EMPTY : new FieldPath(Collections.unmodifiableList(collected));
    }

    /**
     * Returns this path with {@code segment} as its new outermost segment.
     */
    public FieldPath prepend(String segment) {

        return prepended(requireSegment(segment));
    }

    /**
     * Returns this path with an indexed collection element as its new outermost segment, so a
     * failure inside a collection stays addressable: {@code attributes[2].displayName}.
     */
    public FieldPath prepend(String segment, int index) {

        if (index < 0) {

            throw new IllegalArgumentException("Field path index cannot be negative, got " + index);
        }

        return prepended(requireSegment(segment) + "[" + index + "]");
    }

    private FieldPath prepended(String segment) {

        List<String> extended = new ArrayList<>(segments.size() + 1);
        extended.add(segment);
        extended.addAll(segments);

        return new FieldPath(Collections.unmodifiableList(extended));
    }

    public boolean isEmpty() {

        return segments.isEmpty();
    }

    public List<String> getSegments() {

        return segments;
    }

    /**
     * The innermost segment — the one naming the value that actually failed. Empty string when this
     * path carries no location.
     */
    public String getLeaf() {

        return segments.isEmpty() ? "" : segments.get(segments.size() - 1);
    }

    private static String requireSegment(String segment) {

        if (segment == null || segment.trim().isEmpty()) {

            throw new IllegalArgumentException("Field path segment cannot be null or blank");
        }

        return segment;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FieldPath that = (FieldPath) o;
        return Objects.equals(segments, that.segments);
    }

    @Override
    public int hashCode() {

        return Objects.hash(segments);
    }

    @Override
    public String toString() {

        return String.join(".", segments);
    }
}
