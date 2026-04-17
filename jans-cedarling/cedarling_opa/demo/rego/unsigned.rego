package cedarling_opa


default allow := false

unsigned := cedarling_opa.authorize_unsigned(input)

allow if {
    unsigned.decision == true
}
