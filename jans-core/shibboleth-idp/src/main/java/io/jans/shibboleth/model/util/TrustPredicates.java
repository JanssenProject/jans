package io.jans.shibboleth.model.util;

import io.jans.shibboleth.model.EntityIds;
import io.jans.shibboleth.model.ReleasedAttributes;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;

public class TrustPredicates {
    
    private TrustPredicates() {} 

    public static boolean hasAnyActiveProfile(TrustRelationship tr) {

        if (tr == null) return false;

        return isAnyProfileActive(
            tr.getShibbolethSsoProfileConfiguration(),
            tr.getSaml2ArtifactResolutionProfileConfiguration(),
            tr.getSaml2AttributeQueryProfileConfiguration(),
            tr.getSaml2EcpProfileConfiguration(),
            tr.getSaml2SsoProfileConfiguration(),
            tr.getSaml2LogoutProfileConfiguration()
        );
    }

    public static boolean hasAnyActiveProfile(BuildContext ctx) {

        if (ctx == null) return false;

        return isAnyProfileActive(
            ctx.getShibbolethSsoProfileConfiguration(),
            ctx.getSaml2ArtifactResolutionProfileConfiguration(),
            ctx.getSaml2AttributeQueryProfileConfiguration(),
            ctx.getSaml2EcpProfileConfiguration(),
            ctx.getSaml2SsoProfileConfiguration(),
            ctx.getSaml2LogoutProfileConfiguration()
        );
    }

    public static boolean hasRealMetadataSource(TrustRelationship tr) {

        if (tr == null) return false;

        return isRealMetadataSource(tr.getMetadataSource());
    }

    public static boolean hasRealMetadataSource(BuildContext ctx) {

        if (ctx == null) return false;

        return isRealMetadataSource(ctx.getMetadataSource());
    }

    public static boolean hasAnyDiscoveredEntityIds(TrustRelationship tr) {

        if (tr == null) return false;

        return hasAnyEntityId(tr.getDiscoveredEntityIds());
    }

    public static boolean hasAnyDiscoveredEntityIds(BuildContext ctx) {

        if (ctx == null) return false;

        return hasAnyEntityId(ctx.getDiscoveredEntityIds());
    }

    public static boolean hasTrustNature(TrustRelationship tr,TrustNature expected) {

        if (tr == null || expected == null) return false;

        return isTrustNature(tr.getNature(),expected);
    }

    public static boolean hasTrustNature(BuildContext ctx, TrustNature expected) {

        if (ctx == null || expected == null) return false;

        return isTrustNature(ctx.getNature(),expected);
    }

    public static boolean hasNoReleasedAttributes(TrustRelationship tr) {

        return !hasAnyReleasedAttribute(tr.getReleasedAttributes());
    }

    public static boolean hasNoActivationDiagnostics(TrustRelationship tr) {

        return isNoDataActivationDiagnostics(tr.getActivationDiagnostics());
    }

    public static boolean hasNoActivationDiagnostics(BuildContext ctx) {

        return isNoDataActivationDiagnostics(ctx.getActivationDiagnostics());
    }

    public static boolean hasSuccessfulActivationDiagnostics(TrustRelationship tr) {

        return isSuccessfulActivationDiagnostics(tr.getActivationDiagnostics());
    }

    public static boolean hasSuccessfulActivationDiagnostics(BuildContext ctx) {

        return isSuccessfulActivationDiagnostics(ctx.getActivationDiagnostics());
    }

    public static boolean hasFailedActivationDiagnostics(TrustRelationship tr) {

        return isFailedActivationDiagnostics(tr.getActivationDiagnostics());
    }

    public static boolean hasFailedActivationDiagnostics(BuildContext ctx) {

        return isFailedActivationDiagnostics(ctx.getActivationDiagnostics());
    }

    private static boolean isAnyProfileActive(
        ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration,
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration,
        Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration,
        Saml2EcpProfileConfiguration saml2EcpProfileConfiguration,
        Saml2SsoProfileConfiguration saml2SsoProfileConfiguration,
        Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration )  {
        
        return shibbolethSsoProfileConfiguration.getStatus() == ProfileStatus.ACTIVE
            || saml2ArtifactResolutionProfileConfiguration.getStatus() == ProfileStatus.ACTIVE
            || saml2AttributeQueryProfileConfiguration.getStatus() == ProfileStatus.ACTIVE
            || saml2EcpProfileConfiguration.getStatus() == ProfileStatus.ACTIVE
            || saml2SsoProfileConfiguration.getStatus() == ProfileStatus.ACTIVE
            || saml2LogoutProfileConfiguration.getStatus()  == ProfileStatus.ACTIVE;
    }

    private static boolean isRealMetadataSource(MetadataSource source) {

        return source != null && source.getType() != MetadataSourceType.NONE;
    }

    private static boolean hasAnyEntityId(EntityIds entityIds) {

        if (entityIds == null) return false;

        return entityIds.hasAny();
    }

    private static boolean hasAnyReleasedAttribute(ReleasedAttributes attrs) {

        if (attrs == null) return false;

        return attrs.hasAny();
    }

    private static boolean isTrustNature(TrustNature current, TrustNature expected) {

        return current == expected;
    }

    private static boolean isNoDataActivationDiagnostics(ActivationDiagnostics diagnostics) {

        return diagnostics.getStatus() == ActivationStatus.NO_DATA;
    }

    private static boolean isSuccessfulActivationDiagnostics(ActivationDiagnostics diagnostics) {

        return diagnostics.getStatus() == ActivationStatus.SUCCEEDED;
    }

    private static boolean isFailedActivationDiagnostics(ActivationDiagnostics diagnostics) {

        return diagnostics.getStatus() == ActivationStatus.FAILED;
    }
}
