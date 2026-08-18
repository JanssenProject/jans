---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Advisory Denials in a Client-Side PDP

!!! note "Illustrative example"

    Acme Reports and every annotation key below (`@revalidate`, `@stale_context`,
    `@loading_message_id`) are invented for this page. Cedarling attaches no meaning to them; they
    work only because the example application reads them. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme's reporting dashboard is a browser application with Cedarling embedded through the WASM
package. It authorizes locally before it calls the API, which is what makes the interface feel
immediate: buttons that would fail are disabled, and a click that cannot succeed is answered without
a round trip.

The API runs Cedarling too, over the same policy store, and re-evaluates every request. That is not
redundancy. The browser copy exists for the interface, and the server copy is the one that actually
protects the data.

Now two different denials arrive at the same button.

The user is a viewer, and viewers do not generate reports. The browser knows the user's role, the
role is not going to change between the click and the request, and the server would say exactly the
same thing. Rendering "you do not have access" locally is correct and costs nothing.

The workspace has burned through its monthly quota. The browser is comparing against a counter it
fetched when the page loaded, and the real figure lives in the metering service. That number may
have moved in either direction: the month may have rolled over, an administrator may have raised the
plan, or another tab may have consumed the remainder. A local denial here is a guess.

Both are `Deny`. Only one of them is worth showing.

## Why This Is Safe

A client-side PDP can be wrong in exactly one direction. It cannot grant anything, because the
server evaluates the same request again and grants nothing on the browser's say-so. What it can do
is refuse something the server would have allowed, which is a correctness problem in the interface
rather than a security problem.

That asymmetry is what makes the pattern work. Sending the request anyway, after a local denial,
gives away nothing: the authoritative answer is still the server's. The annotation only tells the
browser that its own "no" is not worth rendering.

The reverse must never exist. There is no annotation that lets a client skip the request after a
local `Allow`, and none that lets it disregard a denial from the server. Those would move the
decision to the least trustworthy participant.

## Which Policies Deserve It

Not the important ones. The ones whose conditions rest on facts the browser cannot hold fresh, which
in practice means facts the server owns:

- counters and quotas, like `context.reports_used`
- approval records, like `context.approvals`
- challenge results, like `context.completed_challenges`
- validated incident or session state, like `context.incident_id`
- risk attributes a backend investigation updates, like `principal.risk_tier`

Those examples are the context fields from the other pages in this section, which is not a
coincidence. Every one of them describes something the server established and the client merely
carries a copy of. Their denials are the ones a browser should not render on its own.

A denial that rests on the principal's role, the resource's type, or anything else the browser
received with its tokens is in the opposite position. It is as good locally as it is remotely.

## Schema

```cedarschema
namespace Acme {
    entity User = {
        "role": String,
    };

    entity Workspace;

    action "GenerateReport" appliesTo {
        principal: [User],
        resource: [Workspace],
        context: {
            "reports_used": Long,
        }
    };
}
```

## Policies

The same three policies run in the browser and on the server. Only one of them carries the marker.

```cedar
@id("permit_generate_report")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
);
```

```cedar
@id("forbid_generate_report_wrong_role")
@user_message_id("report.role.required")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
unless {
    ["analyst", "admin"].contains(principal.role)
};
```

```cedar
@id("forbid_generate_report_quota_exhausted")
@revalidate("server")
@stale_context("reports_used")
@loading_message_id("report.checking_quota")
@user_message_id("quota.reports.exhausted")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when { context.reports_used >= 1000 };
```

`@stale_context("reports_used")` is not read by the runtime. It documents *why* the policy is marked,
so the next person to touch it can tell whether the marker is still warranted, and so that removing
`reports_used` from the context is visibly connected to removing the marker.

## The Flow

```text
 ┌──────┐        ┌─────────┐        ┌───────────┐        ┌────────┐
 │ User │        │ Browser │        │  API      │        │Metering│
 └──┬───┘        └────┬────┘        └─────┬─────┘        └───┬────┘
    │  click          │                   │                  │
    │────────────────▶│                   │                  │
    │                 │ local authorize_unsigned             │
    │                 │ (reports_used from page load)        │
    │                 │                   │                  │
    │                 │ Deny, reason: [forbid_..._quota_exhausted]
    │                 │                   │                  │
    │                 │ annotation_values(reason, "revalidate")
    │                 │ ["server"] → do not render this denial
    │  loader         │                   │                  │
    │◀────────────────│                   │                  │
    │                 │  POST /reports    │                  │
    │                 │──────────────────▶│                  │
    │                 │                   │ fresh counter    │
    │                 │                   │─────────────────▶│
    │                 │                   │◀─────────────────│
    │                 │                   │ authorize_unsigned
    │                 │                   │ (authoritative)  │
    │                 │  201, or 403 with its own message    │
    │                 │◀──────────────────│                  │
    │  report / error │                   │                  │
    │◀────────────────│                   │                  │
```

The loader is the honest representation of the browser's state. It does not know the answer, and it
says so.

## Browser Code

None of this belongs in a click handler. The decision of whether a local denial may be rendered is
the same for every guarded action in the application, so it lives in two pieces that every action
shares: a function that reads the decision, and a wrapper that runs the call.

The first one is pure. Given a decision, it says what the browser is entitled to do with it:

```javascript
// The only place the UI interprets an authorization decision.
function localOutcome(result) {
  if (result.decision) {
    return { proceed: true };
  }

  const denials = Object.values(
    cedarling.annotations_by_policy(result.response.diagnostics.reason),
  );

  // A denial the browser is entitled to make, or nothing matched at all.
  const local = denials.find((a) => a.revalidate !== "server");

  if (local || denials.length === 0) {
    return {
      proceed: false,
      messageId: local?.user_message_id ?? "access.denied.generic",
    };
  }

  // Every denial rests on data the server owns, so this one is not ours to render.
  return {
    proceed: true,
    pendingMessageId: denials[0].loading_message_id ?? "common.checking",
  };
}
```

Note that both an `Allow` and a fully provisional `Deny` return `proceed: true`. From the browser's
side they are the same situation: it has nothing worth saying, and the server decides. The only
difference is that one of them warns the interface to expect a wait.

`localOutcome` is also what a render-time check calls. Disabling a button is the interface acting on
a denial before any click, so it may only act on a denial the browser is entitled to make: `proceed:
false` renders as disabled, and everything else renders as enabled and resolves when clicked.

### Bind the Action to the Call It Guards

The wrapper still needs a request to authorize and a call to make, and if a caller supplies those
separately they are free to drift. The browser can authorize `Acme::Action::"GenerateReport"` and
then post to an endpoint that exports raw rows, and nothing complains: the local check was real, it
just answered a question nobody asked. The failure is quiet, because the interface still looks like
it is enforcing something.

Declare the pair once, in a table the whole application shares:

```javascript
const guardedActions = {
  generateReport: {
    action: 'Acme::Action::"GenerateReport"',
    resource: (workspace) => ({
      cedar_entity_mapping: { entity_type: "Acme::Workspace", id: workspace.id },
    }),
    context: (workspace) => ({ reports_used: usage.cached(workspace.id) }),
    call: (workspace) => api.post("/reports", { workspace: workspace.id }),
  },

  exportRows: {
    action: 'Acme::Action::"ExportRows"',
    resource: (workspace) => ({
      cedar_entity_mapping: { entity_type: "Acme::Workspace", id: workspace.id },
    }),
    context: () => ({}),
    call: (workspace) => api.post("/exports", { workspace: workspace.id }),
  },
};

// `target` is what the action operates on, the workspace here. Named `target`
// rather than `subject`, which in authorization usually means the principal.
async function guarded(name, target) {
  const { action, resource, context, call } = guardedActions[name];

  const result = await cedarling.authorize_unsigned(
    JSON.stringify({
      principal,
      action,
      resource: resource(target),
      context: context(target),
    }),
  );

  const outcome = localOutcome(result);

  if (!outcome.proceed) {
    return { status: "denied", messageId: outcome.messageId };
  }

  if (outcome.pendingMessageId) {
    ui.setPendingMessage(outcome.pendingMessageId);
  }

  const response = await call(target);

  return response.ok
    ? { status: "ok", body: response.body }
    : { status: "denied", messageId: response.messageId };  // the server's message
}
```

A handler is then one line, names one thing, and knows nothing about annotations or policy IDs:

```javascript
const outcome = await guarded("generateReport", workspace);
```

Two caveats keep this from becoming a worse idea than it replaces.

The mapping is a declaration, not a naming convention. Cedar actions describe what the user is
doing, so they stay business-shaped: `Acme::Action::"GenerateReport"`, never
`Acme::Action::"POST /reports"`. Naming actions after routes drags transport details into the policy
vocabulary, and every URL change becomes a policy change.

The relationship is not always one to one. A batch endpoint performs several actions, and a
BFF or GraphQL resolver may serve one action through several calls. The rule that survives those
cases is narrower than "one action per endpoint": every call site declares which action it is
guarded by, in one place, next to the call.

The server keeps its own version of this table, and derives the action from its routing rather than
from anything the client sends. The client's binding exists so the interface asks the right
question. The server's binding exists so the answer means something.

The loader needs no special handling. `guarded` returns a promise, and the interface shows a pending
state until it resolves, exactly as it does for any other request. `@loading_message_id` only
supplies better wording for a wait the user was not expecting, since from their side the click
appeared to do nothing locally.

The four cases the browser has to tell apart:

```text
analyst, counter says 10     ALLOW  permit_generate_report
analyst, counter says 1000   DENY   forbid_generate_report_quota_exhausted
                                    marked, so ask the server
viewer,  counter says 10     DENY   forbid_generate_report_wrong_role
                                    unmarked, so render it
viewer,  counter says 1000   DENY   forbid_generate_report_wrong_role
                                    forbid_generate_report_quota_exhausted
                                    one unmarked denial, so render it
```

The direction of that test matters. One unmarked denial is enough to render a refusal, because it
holds regardless of what the server thinks about the others: the viewer without the right role stays
refused even if the quota turns out to be fine. Only when *every* matched policy is marked does the
browser have nothing worth saying.

An empty `reason()` belongs on the same side as an unmarked denial. Nothing permitted the request,
there is no policy to consult about staleness, and a round trip will not change that.

## Server Code

```rust
// The API builds the context from what it owns, not from what the client sent.
let request = report_request(&user, &workspace, metering.reports_used(&workspace)?);
let result = cedarling.authorize_unsigned(request).await?;

if !result.decision {
    let annotations = cedarling.annotations_map(result.response.diagnostics().reason());
    return Err(forbidden(
        annotations
            .get("user_message_id")
            .cloned()
            .unwrap_or_else(|| "access.denied.generic".to_string()),
    ));
}

generate_report(&workspace)
```

The server never reads `@revalidate`. It has nobody to revalidate with. The marker is meaningful
only to a PDP whose answer is provisional, and treating it as meaningful on the server would be a
way of asking a policy to be optional.

That is worth stating in a comment where your server PEP reads annotations, because the shared
policy store makes it easy to assume both sides act on every key.

## Rules That Keep This Honest

**Read the decision in one place.** The rule for when a local denial may be rendered is a property
of the application, not of the action, so it belongs in a single wrapper that every guarded call goes
through. Spread across handlers it will be applied inconsistently, and the handler that forgets it is
the one that shows a user a refusal the server would have overturned.

**Keep the action and the call declared together.** A local check that authorizes one action while
the handler performs another is worse than no check, because it looks like enforcement. Bind them in
one table and let call sites name the binding.

**A loader is not an optimistic yes.** The interface must not enable downstream state, start an
upload, or show a success animation while the request is in flight. It knows nothing yet.

**The final message comes from the server.** After the round trip, show what the server's decision
carried. The local policy that fired may not even be the one that denies at the other end, and its
message may describe a condition that turned out not to apply.

**Do not mark everything.** Each marked policy is a round trip the client-side PDP was supposed to
save. Mark enough policies and the embedded PDP becomes a latency cost with no benefit, at which
point calling the server unconditionally would be simpler and more honest.

**Keep the reason out of the annotation.** `@revalidate("server")` says what the client should do.
Anything explaining *why* the request failed is visible in the browser, including to a user the
policy was written to stop.

**Re-check the marker when the context changes.** A policy marked because it reads a live counter
should lose the marker when it stops reading one. `@stale_context` exists to make that connection
reviewable.

## When to Mark Actions Instead

Marking policies is not the only option. An application can decide per action that a local denial is
never rendered, which needs no annotations at all.

That is the better choice when every denial for an action has the same provenance. Marking policies
earns its keep when one action mixes both kinds, which is the case here: the same button can be
refused for a role the browser knows and for a quota it does not.

## What This Buys You

The interface stops guessing about which failures it is allowed to render. Policy authors decide
that, next to the condition that makes it true, and the same policy store answers both the browser
and the API. A new policy that depends on server-owned data arrives with its own marker, and the
front end handles it without a release.
