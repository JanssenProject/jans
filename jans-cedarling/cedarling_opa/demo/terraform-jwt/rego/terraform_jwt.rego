package infra.terraform_jwt

# Deny by default: Terraform CI operations require an explicit allow.
default allow := false

# Delegate the authorization decision to Cedarling via the multi-issuer interface.
# The input must conform to the authorize_multi_issuer request schema:
#   - tokens:   array of { mapping, payload } objects carrying the verified JWT(s)
#   - action:   one of CI::Action::"Plan" | "Apply" | "Destroy"
#   - resource: the target TerraformWorkspace entity
#   - context:  additional attributes (e.g., current_time)
#
# Cedarling validates the JWT signature against the trusted issuer's JWKS,
# maps the verified claims to a CI::GitHubWorkflow entity (as specified by
# the "mapping" field in the token object), and evaluates the Cedar policies.
result := cedarling.opa.authorize_multi_issuer(input)

# allow is true only when Cedarling returns a positive decision.
allow if {
    result.decision == true
}

# decision mirrors result.decision for direct per-rule access at
# /v1/data/infra/terraform_jwt/decision without unpacking the Cedarling result.
decision := result.decision

# reasons exposes the matching Cedar policy IDs for observability and audit logs.
reasons := result.reasons
