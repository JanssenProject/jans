/** Minimal real Cedar policy store shared by public contract tests. */
export const tracerPolicyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    tracer: {
      cedar_version: "v4.0.0",
      name: "Tracer",
      policies: {
        allow: {
          description: "allow the public tracer",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body: 'permit(principal, action == Tracer::Action::"Read", resource);',
          },
        },
        public: {
          description: "allow a principal-independent public action",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"Public", resource);',
          },
        },
        residual: {
          description: "require a principal attribute for a protected action",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"Protected", resource) when { principal.is_ok };',
          },
        },
        entity_attributes: {
          description: "authorize detached entity attributes",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"Edit", resource) when { principal.role == "editor" && principal.labels.contains("stable") && resource.owner == "alice" };',
          },
        },
        default_entity: {
          description: "authorize a policy-store-owned default resource",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"UseDefault", resource) when { resource.owner == "trusted" };',
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body:
          "namespace Tracer {\n" +
          "entity User = { is_ok?: Bool, role?: String, labels?: Set<String> };\n" +
          "entity Any;\n" +
          "entity Resource = { owner?: String };\n" +
          'action "Read" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          'action "Deny" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          'action "Public" appliesTo { principal: [Any], resource: [Resource], context: {} };\n' +
          'action "Protected" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          'action "Edit" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          'action "UseDefault" appliesTo { principal: [User], resource: [Resource], context: {} };\n' +
          "}",
      },
      default_entities: {
        default:
          "eyJ1aWQiOnsidHlwZSI6IlRyYWNlcjo6UmVzb3VyY2UiLCJpZCI6ImRlZmF1bHQifSwiYXR0cnMiOnsib3duZXIiOiJ0cnVzdGVkIn0sInBhcmVudHMiOltdfQ==",
      },
    },
  },
} as const;

/** Schemaless policy store used to prove canonical extension transport. */
export const tracerExtensionPolicyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    tracer_extensions: {
      cedar_version: "v4.0.0",
      name: "Tracer extensions",
      policies: {
        network: {
          description: "allow callers from the documentation network",
          creation_date: "2026-07-23T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body:
              'permit(principal, action == Tracer::Action::"Connect", resource) when { context.network.isInRange(ip("192.0.2.0/24")) };',
          },
        },
      },
    },
  },
} as const;
