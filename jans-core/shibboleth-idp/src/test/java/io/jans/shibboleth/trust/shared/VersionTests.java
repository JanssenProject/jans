package io.jans.shibboleth.trust.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Version — value & ordering semantics")
public class VersionTests {

    @Test
    @DisplayName("GIVEN the Version factory WHEN initial() is called THEN it holds value 1 and reports isInitial()")
    public void shouldStartAtOne_whenInitial() {

        Version version = Version.initial();

        assertThat(version.getValue()).isEqualTo(1);
        assertThat(version.isInitial()).isTrue();
    }

    @Test
    @DisplayName("GIVEN the Version factory WHEN of(n) is called THEN it holds exactly n")
    public void shouldHoldGivenValue_whenBuiltWithOf() {

        assertThat(Version.of(7).getValue()).isEqualTo(7);
    }

    @Test
    @DisplayName("GIVEN a Version WHEN next() is called THEN a new instance one greater is produced and the original is unchanged")
    public void shouldIncrementAndLeaveOriginalUnchanged_whenNext() {

        Version original = Version.of(3);

        Version next = original.next();

        assertThat(next.getValue()).isEqualTo(4);
        assertThat(next).isNotSameAs(original);
        assertThat(original.getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("GIVEN the initial Version WHEN next() is called THEN the result is no longer initial")
    public void shouldNotBeInitial_afterNext() {

        assertThat(Version.initial().next().isInitial()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a higher and a lower Version WHEN compared with isGreaterThan THEN only the higher reports true")
    public void shouldReportGreaterThan_onlyWhenStrictlyHigher() {

        Version higher = Version.of(5);
        Version lower = Version.of(2);

        assertThat(higher.isGreaterThan(lower)).isTrue();
        assertThat(lower.isGreaterThan(higher)).isFalse();
    }

    @Test
    @DisplayName("GIVEN two equal Versions WHEN compared with isGreaterThan THEN neither is greater than the other")
    public void shouldNotReportGreaterThan_whenEqual() {

        assertThat(Version.of(4).isGreaterThan(Version.of(4))).isFalse();
    }

    @Test
    @DisplayName("GIVEN Versions of different magnitude WHEN compareTo is used THEN it orders them by their numeric value")
    public void shouldOrderByValue_whenCompared() {

        assertThat(Version.of(2).compareTo(Version.of(5))).isNegative();
        assertThat(Version.of(5).compareTo(Version.of(2))).isPositive();
        assertThat(Version.of(4).compareTo(Version.of(4))).isZero();
    }

    @Test
    @DisplayName("GIVEN two Versions holding the same value WHEN compared THEN they are equal and share a hashCode")
    public void shouldBeEqual_whenSameValue() {

        Version one = Version.of(9);
        Version another = Version.of(9);

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    @Test
    @DisplayName("GIVEN two Versions holding different values WHEN compared THEN they are not equal")
    public void shouldNotBeEqual_whenDifferentValue() {

        assertThat(Version.of(1)).isNotEqualTo(Version.of(2));
    }

    @Test
    @DisplayName("GIVEN the Version factory WHEN of(0) is called THEN it is permissive: positivity is enforced upstream, not by Version itself")
    public void shouldNotGuardPositivity_asThatIsEnforcedUpstream() {

        Version zero = Version.of(0);

        assertThat(zero.getValue()).isEqualTo(0);
        assertThat(zero.isInitial()).isFalse();
    }
}
