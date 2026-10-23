package io.jans.shibboleth.trust.config.rules.invariants;

import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.util.BuildContext;
import io.jans.kernel.Result;

public class PresenceInvariants {
    
    private PresenceInvariants() { }

    public static final class IdRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getId() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "id"));
            }
            return Result.success(null);
        }
    }

    public static final class DisplayNameRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getDisplayName() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "displayName"));
            }
            return Result.success(null);
        }
    }

    public static final class DescriptionRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getDescription() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "description"));
            }
            return Result.success(null);
        }
    }

    public static final class NatureRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getNature() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "nature"));
            }

            return Result.success(null);
        }
    }

    public static final class VersionRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getVersion() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "version"));
            }

            return Result.success(null);
        }
    }

    public static final class StatusRequired {

        public static Result<Void> check(BuildContext context) {
            
            if(context.getStatus() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "status"));
            }

            return Result.success(null);
        }
    }

    public static final class MetadataSourceRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getMetadataSource() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "metadataSource"));
            }

            return Result.success(null);
        }
    }

    public static final class ShibbolethSsoProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getShibbolethSsoProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(TrustRelationship.class, "shibbolethSsoProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class Saml2ArtifactResolutionProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getSaml2ArtifactResolutionProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        TrustRelationship.class, "saml2ArtifactResolutionProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class Saml2AttributeQueryProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getSaml2AttributeQueryProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(TrustRelationship.class, "saml2AttributeQueryProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class Saml2EcpProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getSaml2EcpProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(TrustRelationship.class, "saml2EcpProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class Saml2SsoProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getSaml2SsoProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(TrustRelationship.class, "saml2SsoProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class Saml2LogoutProfileConfigurationRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getSaml2LogoutProfileConfiguration() == null) {

                return Result.failure(
                    RequiredValueMissing.forField(TrustRelationship.class, "saml2LogoutProfileConfiguration"));
            }

            return Result.success(null);
        }
    }

    public static final class ReleasedAttributesRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getReleasedAttributes() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "releasedAttributes"));
            }

            return Result.success(null);
        }
    }

    public static final class ActivationDiagnosticsRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getActivationDiagnostics() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "activationDiagnostics"));
            }

            return Result.success(null);
        }
    }

    public static final class DiscoveredEntityIdsRequired {

        public static Result<Void> check(BuildContext context) {

            if (context.getDiscoveredEntityIds() == null) {

                return Result.failure(RequiredValueMissing.forField(TrustRelationship.class, "discoveredEntityIds"));
            }

            return Result.success(null);
        }
    }
}
