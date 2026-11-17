@id("block-untrusted-agent")
@reason("An agent that is not trusted is never allowed to act.")
forbid(principal, action, resource is AcmeOps::Resource)
when { !(context has tokens) && principal is AcmeOps::Agent && !principal.trusted };
