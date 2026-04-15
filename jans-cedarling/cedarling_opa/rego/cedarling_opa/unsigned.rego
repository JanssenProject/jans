package cedarling_opa


default allow := false

allow if {
    result := cedarling_opa.authorize_unsigned(input)
    result.decision == true
}
