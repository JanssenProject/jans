package cedarling_opa


default allow := false

unsigned := cedarling.opa.authorize_unsigned(input)

allow if {
    unsigned.decision == true
}
