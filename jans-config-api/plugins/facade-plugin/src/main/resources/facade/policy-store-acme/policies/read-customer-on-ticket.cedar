@id("read-customer-on-ticket")
@reason("An agent may read only the customer on the ticket assigned to the person it acts for.")
permit(principal, action == AcmeOps::Action::"ReadCustomer", resource is AcmeOps::Resource)
when {
  !(context has tokens) &&
  principal is AcmeOps::Agent &&
  resource.kind == "customer-record" &&
  resource.customer == context.ticket_customer &&
  context.ticket_assignee == principal.acting_for
};
