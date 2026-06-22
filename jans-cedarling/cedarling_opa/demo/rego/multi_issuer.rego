package cedarling_opa


multi_issuer := cedarling.opa.authorize_multi_issuer(input)

allow if {
    multi_issuer.decision == true
}
