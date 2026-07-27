/** Creates a schemaless synthetic policy store for multi-issuer contracts. */
export function createMultiIssuerPolicyStore(
  openidConfigurationEndpoint =
    "https://issuer.example/.well-known/openid-configuration",
) {
  return {
    cedar_version: "v4.0.0",
    policy_stores: {
      multi_issuer: {
        cedar_version: "v4.0.0",
        name: "Multi issuer",
        trusted_issuers: {
          TestIssuer: {
            name: "TestIssuer",
            description: "Synthetic SDK contract issuer",
            openid_configuration_endpoint: openidConfigurationEndpoint,
            token_metadata: {
              access_token: {
                entity_type_name: "Authorization::AccessToken",
              },
            },
          },
        },
        policies: {
          token_present: {
            description: "allow when the mapped access token is present",
            creation_date: "2026-07-23T00:00:00Z",
            policy_content: {
              encoding: "none",
              content_type: "cedar",
              body:
                'permit(principal, action == Authorization::Action::"Read", resource) when { context has tokens && context.tokens has testissuer_accesstoken };',
            },
          },
          default_entity: {
            description: "require policy-store-owned default resource data",
            creation_date: "2026-07-23T00:00:00Z",
            policy_content: {
              encoding: "none",
              content_type: "cedar",
              body:
                'permit(principal, action == Authorization::Action::"Default", resource) when { resource.owner == "trusted" };',
            },
          },
        },
        default_entities: {
          default:
            "eyJ1aWQiOnsidHlwZSI6IkF1dGhvcml6YXRpb246OlJlc291cmNlIiwiaWQiOiJkZWZhdWx0In0sImF0dHJzIjp7Im93bmVyIjoidHJ1c3RlZCJ9LCJwYXJlbnRzIjpbXX0=",
        },
      },
    },
  } as const;
}
