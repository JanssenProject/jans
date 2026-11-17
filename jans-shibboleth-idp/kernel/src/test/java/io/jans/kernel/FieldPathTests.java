package io.jans.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FieldPath — where a failure occurred")
public class FieldPathTests {

    @Test
    @DisplayName("GIVEN no location WHEN inspected THEN the path is empty and renders as nothing")
    public void emptyIsTheAbsenceOfALocation() {

        FieldPath path = FieldPath.empty();

        assertThat(path.isEmpty()).isTrue();
        assertThat(path.getSegments()).isEmpty();
        assertThat(path.getLeaf()).isEmpty();
        assertThat(path.toString()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN a path WHEN a caller prepends its segment THEN the segment becomes outermost")
    public void prependAddsOutermostSegment() {

        FieldPath path = FieldPath.of("validUntil").prepend("metadataSource");

        assertThat(path.getSegments()).containsExactly("metadataSource", "validUntil");
        assertThat(path.getLeaf()).isEqualTo("validUntil");
        assertThat(path.toString()).isEqualTo("metadataSource.validUntil");
    }

    @Test
    @DisplayName("GIVEN a failure inside a collection WHEN indexed THEN the element stays addressable")
    public void prependIndexedAddressesACollectionElement() {

        FieldPath path = FieldPath.of("displayName").prepend("attributes", 2);

        assertThat(path.toString()).isEqualTo("attributes[2].displayName");
    }

    @Test
    @DisplayName("GIVEN a path WHEN prepended THEN the original is unchanged")
    public void prependIsImmutable() {

        FieldPath original = FieldPath.of("displayName");

        original.prepend("trustRelationship");

        assertThat(original.toString()).isEqualTo("displayName");
    }

    @Test
    @DisplayName("GIVEN two paths with the same segments WHEN compared THEN they are equal")
    public void pathsWithEqualSegmentsAreEqual() {

        FieldPath one = FieldPath.of("metadataSource", "validUntil");
        FieldPath other = FieldPath.of("validUntil").prepend("metadataSource");

        assertThat(one).isEqualTo(other);
        assertThat(one.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    @DisplayName("GIVEN a blank or negative segment WHEN used THEN it is rejected as a programming error")
    public void rejectsUnusableSegments() {

        FieldPath fieldpath_one = FieldPath.empty();

        assertThatThrownBy(() -> fieldpath_one.prepend(" "))
            .isInstanceOf(IllegalArgumentException.class);
        FieldPath fieldpath_two = FieldPath.empty();
        assertThatThrownBy(() -> fieldpath_two.prepend(null))
            .isInstanceOf(IllegalArgumentException.class);
        FieldPath fieldpath_three = FieldPath.empty();
        assertThatThrownBy(() -> fieldpath_three.prepend("attributes", -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("GIVEN a path WHEN its segments are read THEN the list cannot be modified")
    public void segmentsAreNotModifiable() {

        FieldPath path = FieldPath.of("displayName");
        List<String> segments = path.getSegments();
        assertThatThrownBy(() -> segments.add("injected"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
