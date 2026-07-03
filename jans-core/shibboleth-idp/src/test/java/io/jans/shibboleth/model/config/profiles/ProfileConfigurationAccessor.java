package io.jans.shibboleth.model.config.profiles;

import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;

import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.contentOf;

import java.util.function.BiFunction;


public class ProfileConfigurationAccessor {

    private final Function<TrustRelationship,Object> extractor;
    private final Function<TrustRelationship,ProfileStatus> trustRelationshipStatusEvaluator;
    private final Function<Object,ProfileStatus> statusEvaluator;
    private final BiFunction<TrustRelationship,Object,TrustResult<TrustRelationship>> mutator;
    private final BiFunction<TrustRelationship.Builder,Object,TrustRelationship.Builder> configurator;

    public static final ProfileConfigurationAccessor SHIBBOLETH_SSO = new ProfileConfigurationAccessor(
        (tr) ->  tr.getShibbolethSsoProfileConfiguration()  ,
        (tr) ->  tr.getShibbolethSsoProfileConfiguration().getStatus(),
        (cnf) -> { return ((ShibbolethSsoProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateShibbolethSsoProfileConfiguration((ShibbolethSsoProfileConfiguration)cnf) ,
        (bld,cnf) -> bld.withShibbolethSsoProfileConfiguration((ShibbolethSsoProfileConfiguration)cnf)
    );

    public static final ProfileConfigurationAccessor SAML2_ARTIFACT_RESOLUTION = new ProfileConfigurationAccessor(
        (tr) -> tr.getSaml2ArtifactResolutionProfileConfiguration(),
        (tr) -> tr.getSaml2ArtifactResolutionProfileConfiguration().getStatus(),
        (cnf) -> { return ((Saml2ArtifactResolutionProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateSaml2ArtifactResolutionProfileConfiguration((Saml2ArtifactResolutionProfileConfiguration)cnf),
        (bld,cnf) -> bld.withSaml2ArtifactResolutionProfileConfiguration((Saml2ArtifactResolutionProfileConfiguration)cnf)
    );

    public static final ProfileConfigurationAccessor SAML2_ATTRIBUTE_QUERY = new ProfileConfigurationAccessor(
        (tr) -> tr.getSaml2AttributeQueryProfileConfiguration(),
        (tr) -> tr.getSaml2AttributeQueryProfileConfiguration().getStatus(),
        (cnf) -> { return ((Saml2AttributeQueryProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateSaml2AttributeQueryProfileConfiguration((Saml2AttributeQueryProfileConfiguration)cnf),
        (bld,cnf) -> bld.withSaml2AttributeQueryProfileConfiguration((Saml2AttributeQueryProfileConfiguration)cnf)
    );

    public static final ProfileConfigurationAccessor SAML2_ECP = new ProfileConfigurationAccessor(
        (tr) -> tr.getSaml2EcpProfileConfiguration(),
        (tr) -> tr.getSaml2EcpProfileConfiguration().getStatus(),
        (cnf) -> { return ((Saml2EcpProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateSaml2EcpProfileConfiguration((Saml2EcpProfileConfiguration)cnf),
        (bld,cnf) -> bld.withSaml2EcpProfileConfiguration((Saml2EcpProfileConfiguration) cnf)
    );

    public static final ProfileConfigurationAccessor SAML2_SSO = new ProfileConfigurationAccessor(
        (tr) -> tr.getSaml2SsoProfileConfiguration(),
        (tr) -> tr.getSaml2SsoProfileConfiguration().getStatus(),
        (cnf) -> { return ((Saml2SsoProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateSaml2SsoProfileConfiguration((Saml2SsoProfileConfiguration)cnf),
        (bld,cnf) -> bld.withSaml2SsoProfileConfiguration((Saml2SsoProfileConfiguration)cnf)
    );

    public static final ProfileConfigurationAccessor SAML2_LOGOUT = new ProfileConfigurationAccessor(
        (tr) -> tr.getSaml2LogoutProfileConfiguration(),
        (tr) -> tr.getSaml2LogoutProfileConfiguration().getStatus(),
        (cnf) -> { return ((Saml2LogoutProfileConfiguration)cnf).getStatus(); },
        (tr,cnf) -> tr.updateSaml2LogoutProfileConfiguration((Saml2LogoutProfileConfiguration)cnf),
        (bld,cnf) -> bld.withSaml2LogoutProfileConfiguration((Saml2LogoutProfileConfiguration)cnf)
    );
    
    private ProfileConfigurationAccessor(
        Function<TrustRelationship,Object> extractor, 
        Function<TrustRelationship,ProfileStatus> trustRelationshipStatusEvaluator,
        Function<Object,ProfileStatus> statusEvaluator,
        BiFunction<TrustRelationship,Object,TrustResult<TrustRelationship>> mutator,
        BiFunction<TrustRelationship.Builder,Object,TrustRelationship.Builder> configurator ) {
        
        this.extractor = extractor;
        this.trustRelationshipStatusEvaluator = trustRelationshipStatusEvaluator;
        this.statusEvaluator = statusEvaluator;
        this.mutator = mutator;
        this.configurator = configurator;
    }

    public final Object extract(TrustRelationship tr) {

        return extractor.apply(tr);
    }

    public final ProfileStatus getStatus(TrustRelationship tr) {

        return trustRelationshipStatusEvaluator.apply(tr);
    }

    public final ProfileStatus getStatus(Object profileconfig) {

        return statusEvaluator.apply(profileconfig);
    }

    public final TrustResult<TrustRelationship> update(TrustRelationship tr, Object profileconfiguration) {

        return mutator.apply(tr, profileconfiguration);
    }

    public final TrustRelationship.Builder configureWithBuilder(TrustRelationship.Builder builder, Object profileconfiguration) {

        return configurator.apply(builder,profileconfiguration);
    }
}
