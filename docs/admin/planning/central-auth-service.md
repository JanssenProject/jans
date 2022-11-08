---
tags:
  - administration
  - planning
---

Older monolithic web access management platforms from the 2000's had
features to allow central policy management--what users can access what
resources. But modern federated digital identity platforms generally do not.
That's true for two reasons. First implementing all the OAuth and OpenID
features is already a wide swath of requirements. Second, policy management,
or authorization, has itself become a nice market. Note the policy management
platforms list in the [RBAC](docs/admin/planning/role-based-access-management.md)
planning guide page.

With that said, if the number of policies is low, there are some ways to
use Jans Auth Server as a central policy decision point. You could use the
presence of OAuth scopes to signal to the policy enforcement point the extent
of access. In this case, policies could be implemented as code in the Update
Token interception script. A similar strategy is to use UMA access tokens
("RPT tokens"), in which case you would use either the RPT interception script
or the claims gathering interception script to implement your policies (or a
combination of both).  But this approach should be used with caution. Most
of the policy management tools today use a declarative language for policies.
Maintaining policies in code does not scale as well.
