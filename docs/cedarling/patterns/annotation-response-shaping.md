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
needs to say is "allowed, in this shape": these columns masked, this many rows, watermarked on the
way out.

Cedar decides *whether*. The annotations carry *in what shape*, and the application enforces it.

## Obligations, Not Hints

The earlier patterns use annotations as hints on a denial, something the user can act on, where the
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
    entity Clearance enum ["standard", "elevated"];

    entity Analyst = {
        "clearance": String,
    };

    entity Dataset = {
        "classification": String,
        "special_category"?: {
            "regulation": String,
            "min_clearance": Clearance,
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

The two clearance fields are typed differently on purpose. `min_clearance` is an enumerated entity
type, so the schema itself lists the legal values and a dataset labeled `"Elevated"` or
`"top_secret"` is rejected before any policy runs:

```text
entity `Acme::Clearance::"top_secret"` is of an enumerated entity type,
but `"top_secret"` is not declared as a valid eid
help: valid entity eids: "standard", "elevated"
```

`Analyst.clearance` stays a `String`, because it arrives from an identity provider and you do not
control what it sends. Typed as an enum, an unexpected claim would fail entity construction and the
request would error out with nothing to show the user. Left as a string, it reaches the policies,
and a guard policy can deny it with a message. Enumerate what you author; guard what you receive.

The tradeoff is that every policy touching an optional attribute has to guard it. `cedar validate`
rejects a policy that reaches for `resource.special_category.regulation` without the `has` check, so
run it in CI. Cedarling does not validate policies when it authorizes: an unguarded policy errors at
request time, drops out of the decision, and takes its annotations with it.

## Policies

The baseline grant is deliberately narrow: standard clearance gets masked columns and a small page
of rows.

`@mask` carries a delimited list rather than one key per column, because Cedar allows a key to
appear only once per policy. That is the one place where packing several values into a string is the
right call. It stays a flat list: no nested structure, and nothing for the application to parse
beyond splitting on a comma.

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
when {
    resource has special_category &&
    ["standard", "elevated"].contains(principal.clearance)
};
```

The clearance test in that policy is doing real work. Without it the permit fires for any principal
at all, so an unrecognized clearance value would be allowed on the most sensitive datasets while
still being denied on ordinary ones, which is the opposite of what anyone intends. A grant that
turns on a property of the *resource* still has to say who it is for.

Elevated clearance needs its own baseline, or an elevated analyst doing ordinary work matches no
permit and falls to the default deny with nothing to show them:

```cedar
@id("permit_query_elevated")
@mask("national_id")
@row_limit("10000")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    principal.clearance == "elevated" &&
    context.purpose != "incident_response"
};
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
    resource.special_category.min_clearance == Acme::Clearance::"elevated" &&
    ["standard", "elevated"].contains(principal.clearance) &&
    principal.clearance != "elevated"
};
```

An unrecognized clearance on the principal is a plain default deny with nothing to show, so it needs
a guard of its own:

```cedar
@id("forbid_query_unrecognized_clearance")
@user_message_id("query.clearance.unrecognized_principal")
@escalate_to("identity-governance")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    !(["standard", "elevated"].contains(principal.clearance))
};
```

The dataset side needs no such guard. `min_clearance` is an enumerated type, so a value outside the
list never reaches a policy: it is rejected when the entity is built. That is the whole reason to
spend an enum on it.

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

A standard analyst querying a dataset that carries `special_category` with `min_clearance` of
`"standard"` satisfies two of these policies, so both appear in the decision:

```text
ALLOW  reason: permit_query_standard
               permit_query_special_category
```

This is where obligations differ sharply from hints. On a denial, reading any one annotation is
usually fine, because every matched `forbid` describes something the user has to do. On an `Allow`,
the matched policies each describe a limit, and taking one of them means dropping the others.

`annotations_map` is the wrong tool here. It keeps one arbitrary value per key, so with two `@mask`
annotations in play it returns one of them and drops the other, and which one it drops is
arbitrary, so the health column may be the one that ships in the clear.

Merge restrictively instead: union the masks, take the smallest row limit, and treat a watermark
requirement as sticky. Where no matching policy says anything, fall back to the application's own
floor rather than to no limit at all.

```rust
// Reached on an Allow. A Deny never gets here; the denial path is its own branch.
let reason: Vec<_> = result.response.diagnostics().reason().collect();

// Union: the columns this deployment always masks, plus anything a matched policy adds.
// A column name the response builder does not know is a broken obligation: masking
// it would be a no-op, and the column would ship in the clear.
let mut masked: BTreeSet<String> = baseline_masked_columns();
for value in cedarling.annotation_values(reason.iter().copied(), "mask") {
    for column in value.split(',').map(str::trim) {
        if !KNOWN_COLUMNS.contains(column) {
            return Err(Error::MalformedObligation("mask", column.to_string()));
        }
        masked.insert(column.to_string());
    }
}

// Minimum of what the matched policies asked for. If none of them asked, the
// application's default applies: an absent obligation must never widen anything.
// A value that does not parse is a broken obligation, not a reason to fall back.
let mut row_limit = None;
for value in cedarling.annotation_values(reason.iter().copied(), "row_limit") {
    let parsed: usize = value
        .parse()
        .map_err(|_| Error::MalformedObligation("row_limit", value))?;
    row_limit = Some(row_limit.map_or(parsed, |current: usize| current.min(parsed)));
}
let row_limit = row_limit.unwrap_or(DEFAULT_ROW_LIMIT);

// Sticky, and only a value we recognize counts. Anything else stops the response.
let mut watermark = WATERMARK_BY_DEFAULT;
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

The starting values are easy to get wrong. A union starts from the empty set, which masks nothing,
and a minimum starts from an unbounded ceiling, which limits nothing. Start there and any request
that matches one permit carrying no `@row_limit` is served with no row limit at all.

This policy set does not produce that, and only because every clearance has a baseline grant that
carries a limit. Delete `permit_query_elevated` and an elevated analyst on a special-category
dataset matches `permit_query_special_category` alone, which masks a column and says nothing about
volume. A single missing baseline produces it. With the fallbacks anchored in the application, a
missing annotation leaves the floor where it is instead of lifting it.

Both loops fail closed. `unwrap_or(DEFAULT_ROW_LIMIT)` on a value that failed to parse would
silently widen the response, and treating any non-empty `@watermark` as true would let
`@watermark("false")` turn watermarking on. An obligation the application cannot interpret is a
policy the application is not implementing, so refuse the query and let the error reach whoever
edits the policies.

The incident-response policy shows why the direction of the merge matters. Its `@row_limit` of
100,000 is wider than the 10,000 an elevated analyst normally gets, and a minimum merge cannot widen
anything: if both policies matched, the analyst would be held at 10,000 no matter what the wider one
says. That is why `permit_query_elevated` carries `context.purpose != "incident_response"`. The
exclusion is what makes the wide grant reachable.

Widening access therefore comes from *not* matching the narrow policy, never from adding a broader
one alongside it. Whenever you write a permit that is meant to grant more, check what else the same
request matches, because under a restrictive merge the narrowest matched policy is the one that
decides.

A useful sanity check when adding a `permit` with obligations: if this policy matched together with
every other `permit`, would the merged result still be safe? If the answer depends on which policy
the application happens to read, the merge logic is not restrictive enough.

## Mapping Policies to Controls

The keys in these policies are not all for the runtime. `@control("SOC2-CC6.1")`,
`@regulation("GDPR-Art9")`, and `@retention_days("30")` are read by people and by tooling that never
makes an authorization call.

This is a different use of the same mechanism, and it changes what the annotation is for. The
runtime keys tell the application what to do with one response. The compliance keys describe the
policy itself, so that:

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
