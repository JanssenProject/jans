package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Value-equality contracts for the profile configurations. Regression tests for two copy-paste defects:
 * Shibboleth SSO's hashCode omitted samlConfigurationSupport (and double-counted the assertion support),
 * and SAML2 artifact-resolution's equals compared samlConfigurationSupport to itself. Both let a change
 * confined to samlConfigurationSupport go undetected, which in turn skips the aggregate's version bump.
 */
@DisplayName("Profile Configuration — value equality")
public class ProfileConfigurationEqualityTests {

    private ShibbolethSsoProfileConfiguration shibbolethSso(MessageSigningPolicy signing) {

        return ShibbolethSsoProfileConfiguration.from(SamlProfileConfigurationDefaults.shibbolethSso())
            .messageSigningPolicy(signing)
            .build()
            .getValue();
    }

    private Saml2ArtifactResolutionProfileConfiguration artifactResolution(MessageSigningPolicy signing) {

        return Saml2ArtifactResolutionProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .messageSigningPolicy(signing)
            .build()
            .getValue();
    }

    @Test
    @DisplayName("GIVEN two Shibboleth SSO configs equal in every field THEN they are equal with equal hashCodes")
    public void shibbolethSso_equalConfigsShareHashCode() {

        ShibbolethSsoProfileConfiguration a = shibbolethSso(MessageSigningPolicy.SIGN_BOTH);
        ShibbolethSsoProfileConfiguration b = shibbolethSso(MessageSigningPolicy.SIGN_BOTH);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("GIVEN two Shibboleth SSO configs differing only in message-signing policy THEN their hashCodes differ")
    public void shibbolethSso_hashCodeReflectsSamlConfiguration() {

        ShibbolethSsoProfileConfiguration signBoth = shibbolethSso(MessageSigningPolicy.SIGN_BOTH);
        ShibbolethSsoProfileConfiguration signNone = shibbolethSso(MessageSigningPolicy.SIGN_NONE);

        assertThat(signBoth).isNotEqualTo(signNone);
        assertThat(signBoth.hashCode()).isNotEqualTo(signNone.hashCode());
    }

    @Test
    @DisplayName("GIVEN two artifact-resolution configs differing only in message-signing policy THEN they are not equal")
    public void artifactResolution_equalsReflectsSamlConfiguration() {

        Saml2ArtifactResolutionProfileConfiguration signBoth = artifactResolution(MessageSigningPolicy.SIGN_BOTH);
        Saml2ArtifactResolutionProfileConfiguration signNone = artifactResolution(MessageSigningPolicy.SIGN_NONE);

        assertThat(signBoth).isNotEqualTo(signNone);
    }

    @Test
    @DisplayName("GIVEN two artifact-resolution configs equal in every field THEN they are equal with equal hashCodes")
    public void artifactResolution_equalConfigsShareHashCode() {

        Saml2ArtifactResolutionProfileConfiguration a = artifactResolution(MessageSigningPolicy.SIGN_BOTH);
        Saml2ArtifactResolutionProfileConfiguration b = artifactResolution(MessageSigningPolicy.SIGN_BOTH);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
