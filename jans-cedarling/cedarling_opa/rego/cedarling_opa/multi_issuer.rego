package cedarling_opa


signed := cedarling_opa.authorize(input)

allow if {
    signed.decision == true
}
