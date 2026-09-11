# Decomposition and reuse

Agama is capable of dealing with complexity. As in other languages, Agama allows to break down problems into smaller ones that can be tackled more easily. The opposite approach is applicable too: "sophisticated" solutions can be built by "orchestrating" existing flows that solve less challenging problems. In other words, Agama allows composition and flow reuse as it will be shown here.

An Agama flow can be understood as a unit that solves a specific problem. Think of a function/method in regular programming. If well-designed, it can be invoked from some other flow or flows to make more interesting stuff. Invocation in Agama is performed through the [`Trigger`](https://docs.jans.io/stable/agama/language-reference/#subflows) directive. It is equivalent to `Call` except it is not for foreign but native Agama code.

## The club revisited

The unforgiving club has been helpful to illustrate Agama basics like [conditionals](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/conditionals-matching/#example-the-unforgiving-club) and [looping](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/loops/#club-referrals); and it is back again - for good. Tired of emitting admission approvals manually, the board decided to accept anyone having what they consider the "right" amount of personal interests. This way, acceptance can be done automatically and the compilation of referrals can be done in a single online process. That's very good news!

Flow [`com.acme.basic.club_application2`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/conditionals-matching/project/code/com.acme.basic.club_application2.flow) already handles admissions while [`com.acme.basic.referrals`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/loops/project/code/com.acme.basic.referrals.flow) does the referrals stuff. All that's left is gluing these together:

```
Flow com.acme.workaday.club_admission1
    Basepath ""

obj = Trigger com.acme.basic.club_application2
When obj.success is true
   Trigger com.acme.basic.referrals
   Finish true

Finish false
```

It's straightforward: flow `com.acme.basic.club_application2` is launched (triggered), if it finishes successfully `com.acme.basic.referrals` is subsequently triggered and then the process ends positively, otherwise `com.acme.workaday.club_admission1` terminates with failure.

See the outcome of `com.acme.basic.club_application2` is assigned to variable `obj`. Recall `Finish true` is merely a compact form of ending a flow supplying a *map* like `{ success: true }` to its caller. That's the reason the conditional reads `When obj.success is true` instead of `When obj is true`. Learn more about `Finish` [here](https://docs.jans.io/stable/agama/language-reference/#flow-finish).

The outcome of `com.acme.basic.referrals` is ignored because that flow always terminates successfully. This is uncommon in practice though: most flows either terminate successfully or failed, and even attach extra data for their callers to do some form of post-processing.

## Improvements

Flow `com.acme.basic.club_application2` hard-codes important facts: the available interests and the minimum and maximum number of them that are acceptable for being admitted. Flow [`com.acme.workaday.club_application`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/decomposition-reuse/project/code/com.acme.workaday.club_application.flow) is an enhanced version that makes use of input parameters:

```
Flow com.acme.workaday.club_application
    Basepath ""
    Inputs interests minAllowed maxAllowed

data = { interests: interests, min: minAllowed, max: maxAllowed }
regData = RRF "application.ftlh" data

list = regData.interest
When list is null
    len = 0
Otherwise
    len = list.length

arr = [ len, minAllowed, maxAllowed ]
pq = Call java.util.PriorityQueue#new arr
Call pq poll
mid = Call pq poll

When mid is not len
    RRF "rejected.ftlh"
    Finish false

obj = { success: true, data: { name: name } } 
Finish obj
```

Variables `interests`, `minAllowed`, and `maxAllowed` will hold the values the caller will pass: a *list* of *string*s, and two (integer) *number*s. The value of variable `len` is the number of interests selected by the user in the form found in [`application.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/decomposition-reuse/project/web/application.ftlh). This template is not the same file used in the former referrals flow. It's a file found under the current [project](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/decomposition-reuse/project).

Then calculation of `mid` comes. This is clever way to determine if `minAllowed <= len <= maxAllowed` is true, which is the criterion for club acceptance: think of sorting an array whose elements are `len`, `minAllowed`, `maxAllowed` and then accessing the element in the second (middle) position. Such value should be equal to `len`. This computation is carried out with the help of Java's [`PriorityQueue`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PriorityQueue.html).

When the acceptance criterion is fulfilled, the flow terminates successfully, and additionally the name of the applicant is attached so the caller can do something with it afterward.

Template [`rejected.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/decomposition-reuse/project/web/rejected.ftlh) is a variation of the original [`rejected2.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/conditionals-matching/project/web/rejected2.ftlh) template.

Regarding `com.acme.basic.referrals`, the flow will be re-used as is.

A new version of `club_admission1` comes now:

```
Flow com.acme.workaday.club_admission2
    Basepath ""
    Configs conf

obj = Trigger com.acme.workaday.club_application conf.interests conf.allowed.min conf.allowed.max
When obj.success is true
   Log "Applicant % has been admitted" obj.data.name
   Trigger com.acme.basic.referrals
   Finish true

Finish false
```

Note the `Trigger` call for `com.acme.workaday.club_application` is passing the three params expected by such flow, in order. The values originate from configuration properties referenced via `conf` variable.

This can be tested once proper configurations has been set for the project. File [`project.json`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/decomposition-reuse/project/project.json) provides an example in its `configs` section. Ensure the [loops](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/loops/project) project is still deployed (so `com.acme.basic.referrals` can be triggered successfully).
