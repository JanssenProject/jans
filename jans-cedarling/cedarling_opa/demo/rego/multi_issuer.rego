package cedarling_opa


multi_issuer := cedarling_opa.authorize(input)

allow if {
    multi_issuer.decision == true
}
