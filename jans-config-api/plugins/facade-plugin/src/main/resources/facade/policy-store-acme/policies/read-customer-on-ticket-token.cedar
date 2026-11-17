@id("read-customer-on-ticket-token")
@reason("A signed-in user may read only the customer on the ticket assigned to them.")
permit(principal, action == AcmeOps::Action::"ReadCustomer", resource is AcmeOps::Resource)
when {
  context has tokens &&
  context.tokens has acme_userinfo_token &&
  context.tokens.acme_userinfo_token has given_name &&
  resource.kind == "customer-record" &&
  resource.customer == context.ticket_customer &&
  context.ticket_assignee == context.tokens.acme_userinfo_token.given_name
};
