package io.jans.shibboleth.model.rules.invariants;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.TrustResult;

public class PresenceInvariants {
    
    private PresenceInvariants() { }

    public static final class IdRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getId() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("id"));
            }
            return TrustResult.success(null);
        }
    }

    public static final class DisplayNameRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getDisplayName() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("displayName"));
            }
            return TrustResult.success(null);
        }
    }

    public static final class DescriptionRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getDescription() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("description"));
            }
            return TrustResult.success(null);
        }
    }

    public static final class NatureRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getNature() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("nature"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class VersionRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getVersion() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("version"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class StatusRequired {

        public static TrustResult<Void> check(BuildContext context) {
            
            if(context.getStatus() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("status"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class MetadataSourceRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getMetadataSource() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("metadataSource"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class ShibbolethSsoProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getShibbolethSsoProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("shibbolethSsoProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class Saml2ArtifactResolutionProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getSaml2ArtifactResolutionProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("saml2ArtifactResolutionProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class Saml2AttributeQueryProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getSaml2AttributeQueryProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("saml2AttributeQueryProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class Saml2EcpProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getSaml2EcpProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("saml2EcpProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class Saml2SsoProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getSaml2SsoProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("saml2SsoProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class Saml2LogoutProfileConfigurationRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getSaml2LogoutProfileConfiguration() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("saml2LogoutProfileConfiguration"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class ReleasedAttributesRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getReleasedAttributes() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("releasedAttributes"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class ActivationDiagnosticsRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getActivationDiagnostics() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("activationDiagnostics"));
            }

            return TrustResult.success(null);
        }
    }

    public static final class DiscoveredEntityIdsRequired {

        public static TrustResult<Void> check(BuildContext context) {

            if (context.getDiscoveredEntityIds() == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("discoveredEntityIds"));
            }

            return TrustResult.success(null);
        }
    }
}
