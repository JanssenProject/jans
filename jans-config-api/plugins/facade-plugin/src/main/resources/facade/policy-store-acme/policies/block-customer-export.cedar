@id("block-customer-export")
@reason("Bulk customer exports are never allowed for an agent.")
forbid(principal, action == AcmeOps::Action::"ExportCustomers", resource is AcmeOps::Resource);
