@id("block-suspicious-request")
@reason("A request flagged as suspicious, such as a likely prompt injection, is blocked.")
forbid(principal, action, resource is AcmeOps::Resource)
when { context.suspicious };
