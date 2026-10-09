package infra.terraform

# Deny by default: Terraform operations require an explicit allow.
default allow := false

# Delegate the authorization decision to Cedarling via the unsigned interface.
result := cedarling.opa.authorize_unsigned(input)

# allow is true only when Cedarling returns a positive decision.
allow if {
    result.decision == true
}

# decision mirrors result.decision for direct access at /v1/data/infra/terraform/decision.
decision := result.decision

# reasons contains the IDs of Cedar policies that granted the request.
reasons := result.reasons
