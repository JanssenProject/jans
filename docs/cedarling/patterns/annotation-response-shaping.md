---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Shaping the Response and Mapping Controls

!!! note "Illustrative example"

    Acme Analytics and every annotation key below (`@mask`, `@row_limit`, `@control`, and the rest)
    are invented for this page. Cedarling attaches no meaning to them; they work only because the
    example application reads them. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme Analytics lets internal staff query customer datasets. Access is not really a yes or no
question. A support agent looking up one account should see the account, but not the customer's
national ID. An analyst studying churn needs a hundred thousand rows and no personal identifiers at
all. An on-call engineer during an incident needs the raw record, and the company needs a record of
the fact.

Answering with `Deny` in all the awkward cases pushes people toward CSV exports and shared logins.
Answering with an unrestricted `Allow` gives away more than the request needed. What the system
actually wants to say is "allowed, in this shape": these columns masked, this many rows, watermarked
on the way out.

Cedar decides *whether*. The annotations carry *in what shape*, and the application enforces it.

## Obligations, Not Hints

The earlier patterns use annotations as hints on a denial. Something the user can act on, and the
worst case of ignoring one is a poor experience. This pattern is different. A `@mask("national_id")`
on a `permit` is an obligation: if the application ignores it, the data goes out unmasked and the
policy that looked like a control did nothing.

Two rules follow from that, and they are the reason this pattern needs care:

- Enforce obligations in one place, close to where the response is built. An obligation applied in
  three handlers is an obligation missing from the fourth.
- Fail closed on an unknown key. If a policy carries `@mask` and the application does not recognize
  the column name, mask everything or refuse, and never return the row unmasked because the
  instruction was unfamiliar.

## Schema

```cedarschema
namespace Acme {
    entity Analyst = {
        "clearance": String,
    };

    entity Dataset = {
        "classification": String,
        "special_category"?: {
            "regulation": String,
            "min_clearance": String,
        },
    };

    action "Query" appliesTo {
        principal: [Analyst],
        resource: [Dataset],
        context: {
            "purpose": String,
        }
    };
}
```

`special_category` is optional, and the policies test it with `resource has special_category`. A
dataset that holds nothing regulated simply does not carry the attribute, which reads better than a
`Bool` flag and gives the extra fields somewhere to live: the regulation the data falls under and
the clearance it demands travel with the dataset instead of being hardcoded in whichever policy
happens to mention it.

The tradeoff is that every policy touching an optional attribute has to guard it. Cedar's validator
enforces that, so a policy that reaches for `resource.special_category.regulation` without the
`has` check is rejected at validation time rather than erroring at request time.

## Policies

The baseline grant is deliberately narrow: standard clearance gets masked columns and a small page
of rows.

```cedar
@id("permit_query_standard")
@mask("national_id,date_of_birth")
@row_limit("100")
@watermark("true")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when { principal.clearance == "standard" };
```

Special-category data carries its own rule, and it applies no matter who is asking:

```cedar
@id("permit_query_special_category")
@mask("diagnosis_code")
@regulation("GDPR-Art9")
@retention_days("30")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when { resource has special_category };
```

Clearance below what the dataset demands is a denial rather than a narrower grant, and the dataset
states the requirement itself:

```cedar
@id("forbid_query_below_required_clearance")
@user_message_id("query.clearance.insufficient")
@request_access_route("access.request_clearance")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    resource has special_category &&
    resource.special_category.min_clearance == "elevated" &&
    principal.clearance != "elevated"
};
```

`min_clearance` is a plain `String`, and that policy only recognizes one value. A dataset asking for
`"top_secret"`, or for `"Elevated"` with a stray capital, matches no denial and still matches the
permits, so a stricter requirement would read as no requirement at all. Close that the same way the
quota page closes an unrecognized tier:

```cedar
@id("forbid_query_unrecognized_clearance_requirement")
@user_message_id("query.clearance.unrecognized")
@escalate_to("data-governance")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    resource has special_category &&
    !(["standard", "elevated"].contains(resource.special_category.min_clearance))
};
```

Any value the policy set does not know about now denies the query and tells someone to fix the
dataset. The alternative is a closed vocabulary in the schema, which Cedar cannot express for a
`String` attribute, so the guard policy is where that check has to live.

Incident response gets the wider grant, and pays for it in audit:

```cedar
@id("permit_query_incident_response")
@row_limit("100000")
@audit("full_query_text")
@notify("data_protection_officer")
@control("SOC2-CC7.2")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    principal.clearance == "elevated" &&
    context.purpose == "incident_response"
};
```

## Overlapping Permits Need a Restrictive Merge

A standard analyst querying a dataset that carries `special_category` satisfies two of these
policies, so both appear in the decision:

```text
ALLOW  reason: permit_query_standard
               permit_query_special_category
```

This is where obligations differ sharply from hints. On a denial, reading any one annotation is
usually fine, because every matched `forbid` describes something the user has to do. On an `Allow`,
the matched policies each describe a limit, and taking one of them means dropping the others.

`annotations_map` is the wrong tool here. It keeps one arbitrary value per key, so with two `@mask`
annotations in play it would return one of them and silently discard the other, which in this case
means shipping the health column in the clear.

Merge restrictively instead: union the masks, take the smallest row limit, and treat any watermark
or audit requirement as sticky.

```rust
let reason: Vec<_> = result.response.diagnostics().reason().collect();

// Union: every column any matching policy wants masked.
let masked: BTreeSet<String> = cedarling
    .annotation_values(reason.iter().copied(), "mask")
    .iter()
    .flat_map(|v| v.split(','))
    .map(|c| c.trim().to_string())
    .collect();

// Minimum: the tightest limit wins. A value that does not parse is a broken
// obligation, not a reason to fall back to a wider default.
let mut row_limit = HARD_ROW_CEILING;
for value in cedarling.annotation_values(reason.iter().copied(), "row_limit") {
    let parsed: usize = value
        .parse()
        .map_err(|_| Error::MalformedObligation("row_limit", value))?;
    row_limit = row_limit.min(parsed);
}

// Sticky, but only a value we recognize counts. Anything else stops the response.
let mut watermark = false;
for value in cedarling.annotation_values(reason.iter().copied(), "watermark") {
    match value.as_str() {
        "true" => watermark = true,
        "false" => {}
        _ => return Err(Error::MalformedObligation("watermark", value)),
    }
}

let rows = run_query(&query, row_limit)?;
Ok(shape(rows, &masked, watermark))
```

Both loops fail closed, which is the point. `unwrap_or(DEFAULT_ROW_LIMIT)` on a value that failed to
parse would silently widen the response, and treating any non-empty `@watermark` as true would let
`@watermark("false")` turn watermarking on. An obligation the application cannot interpret is a
policy the application is not implementing, so refuse the query and let the error reach whoever
edits the policies.

The incident-response policy shows why the direction of the merge matters. Its `@row_limit` of
100,000 is wider than the standard 100, and if the analyst also matches the standard policy, the
minimum keeps them at 100. Widening access has to come from *not* matching the narrow policy, not
from adding a broader one alongside it. Write the wide grant so that the narrow one cannot match at
the same time, or accept that the narrow limit wins.

A useful sanity check when adding a `permit` with obligations: if this policy matched together with
every other `permit`, would the merged result still be safe? If the answer depends on which policy
the application happens to read, the merge logic is not restrictive enough.

## Mapping Policies to Controls

The keys in these policies are not all for the runtime. `@control("SOC2-CC6.1")`,
`@regulation("GDPR-Art9")`, and `@retention_days("30")` are read by people and by tooling that never
makes an authorization call.

This is a different use of the same mechanism, and it is worth naming because it changes what the
annotation is for. The runtime keys tell the application what to do with one response. The
compliance keys describe the policy itself, so that:

- an auditor asking "show me the controls that enforce CC6.1" gets an answer from the policy store,
  by grepping for `@control("SOC2-CC6.1")`, rather than from a spreadsheet somebody maintains by
  hand
- the decision log records which control was exercised, because the annotations resolve from the
  same `reason()` the log already carries
- retention tooling reads `@retention_days` from the policy that authorized the query, instead of
  inferring a retention class from the dataset name

```rust
// Written to the audit trail alongside the decision.
audit.record(Access {
    decision: result.decision,
    controls: cedarling.annotation_values(reason.iter().copied(), "control"),
    regulations: cedarling.annotation_values(reason.iter().copied(), "regulation"),
    by_policy: cedarling.annotations_by_policy(reason.iter().copied()),
});
```

`annotations_by_policy` is the right call for the audit record. It keeps the attribution intact, so
the log says *which* policy claimed CC6.1 rather than merely that something did.

Keep the compliance vocabulary as disciplined as the runtime vocabulary. A control ID that no longer
exists in the control framework, attached to a policy nobody has reviewed, is worse than no
annotation: it produces a confident, wrong answer at audit time.

## What This Buys You

The data-handling rules stop being scattered through query builders and serializers. Which columns
are sensitive, how many rows a clearance level may pull, what gets watermarked, and which control
each rule implements are all stated in the policy that grants the access. The application keeps one
implementation of masking, limiting, and watermarking, and applies whatever the decision asked for.
