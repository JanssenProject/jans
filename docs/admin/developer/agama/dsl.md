---
tags:
  - administration
  - developer
  - agama
---

# Agama DSL

!!! Note
    This document presents a view of the most relevant aspects and constructs of the language. The [Full reference](./dsl-full.md) provides a more detailed insight on the different DSL elements.

## Introduction

Agama flows are written using a small DSL (domain specific language) designed purposedly for writing web flows. Some remarkable features of the DSL include:

- It helps depicting the structure of web flows in a natural way
- It is closer to human language than general-purpose programming languages
- It has a clean, concise, non-distracting syntax
- It has by design limited computational power forcing computations to occur in a more formal, general-purpose language like Java

Intrinsic properties to highlight:

- It follows the imperative paradigm mainly, and makes use of a few declarative elements
- Execution takes place in a traditional sequential manner
- Flows can be treated as functions (reusable routines with well-defined inputs)
- It has no special constructs focused on authentication semantics: a flow simply finishes with a positive or negative result (plus optional extra data), however
- It provides dedicated contructs for common patterns in web flows like:
    - "show a page" and "retrieve the data user provided in that page"
    - "redirect a user to an external site" and later "retrieve the data provided at the callback url"
- It supports typical language elements like assignments, conditionals, loops, etc.

!!! Important
    In the process of flow writing, the DSL should be mainly used to structure a flow and collect user data while delegating more intensive tasks like querying databases, invoking web services, and so on, to Java. 

## Language compiler

Agama is not a compiled language. Code is transpiled to an intermediate representation which is then interpreted at runtime. Developers don't need to issue special commands for transpilation to occur. This is automatically executed in the background by the engine some seconds after a flow has been added or modified.

The process of checking and fixing potential syntax errors is described [here](./lifecycle.md#about-syntax-errors).

## Syntactic features

- Agama is case sensitive, e.g. `X` and `x` are two different things
- With a few exceptions, instructions always have to be written in a **single line**
- There are no parenthesis or semicolons
- Nesting of blocks takes place by using indented blocks
- Single line comments are supported. Use `//` to start a comment anywhere in a given line

## Data types, literals and variables

Agama is a dynamically typed language. Data types are not explicit in code and variables are not declared, just used. Type checks occur at runtime.

Values fit into any of: _string_, _boolean_, _number_, _list_ or _map_. Also, the "special" value `null` can be used to represent the absence of a value. The table below presents examples of literals:

|Type|Valid literals|Invalid literals|
|-|-|-|
|string|"Agama"; ""; "Hello\nGluu" (line feed usage)|"""|
|boolean|true; false|True; yes; FALSE|
|number|0.1; -0.1; 1; 1000; 255|.1; -.1; +1; 1e+03; 0xFF|
|list|[]; [1, 2, 3]; [1, [2, 3]]; ["Agama", false]|[1..3]; [1..]|
|map|{}; { Color: "blue" }, { a: { a_b: ["foo"] } }|{ _a: 2 }; { 2: 2 }; { "a_b": 2 }|

See the [full reference](./dsl-full.md#literals) to learn more about literals.

A variable is global in the flow where it is being used. Examples of valid variable names are: `x`, `mom`, `Yes`, `A_ha`, `B52`. The following are invalid: `4`, `"red"`, `_oh`, `A-ha`.

Variables can be assigned a value several times in the same flow. Examples of valid assignments: `x = 0`, `mom = "love"`, `Yes = true`, `A_ha = [ 2.718, 3.141 ]`, `B52 = { rock: "lobster" }`

Depending on the data type of a variable, its underlying value(s) can be accessed or modified in a variety of ways. As shown in the table bellow Agama feels pretty natural:

|Sample assignment|Access/Mutation example|Result|
|-|-|-|
|`x = "Hi"`|`x[0]`|`"H"`|
|`x = "Hi"`|`x.length`|`2`|
|`x = "Hi"`|`x[1] = "o"`|Strings are immutable|
|`x = [1, 2, 3]`|`x[1]`|`2`|
|`x = [1, 2, 3]`|`x[3]`|`null`|
|`x = [1, 2, 3]`|`x[0] = "zero"`|`["zero", 2, 3]`|
|`x = [1, 2, 3]`|`x.length = 5`|`["zero", 2, 3, null, null]`|
|`x = { a: true }`|`x.a`|`true`|
|`x = { a: true }`|`x.b`|`null`|
|`x = { a: true }`|`x.c = false`|`{ a: true, c: false }`|

A variable whose value is the product of a Java call can be accessed and manipulated as described [here](./dsl-full.md#java-objects).

See the [full reference](./dsl-full.md#accessing-and-mutating-data-in-variables) to learn more about accessing and mutating data in variables.

## Flow structure

A flow consists of a header and one or more statements following. The header starts with the `Flow` keyword followed by the qualified name of the flow, e.g. `Flow com.acme.FoodSurvey`.

Next, in an indented block several aspects can be provided (in this specific order):

- The base directory to use for this flow's assets
- Flow timeout
- A variable to hold flow configurations
- Variables to receive flow inputs

Except for the base directory, all of these are optional. 

The following is an example of a flow header, where the folder `mydir` should hold the assets of the flow (more info on assets [here](../../../admin/developer/agama/quick-start.md)). The flow may receive three parameters: `salutation`, `askGender`, and `promptRealName` from their callers (when used as [subflow](#subflows)) or from the [authentication request](./quick-start.md#craft-an-authentication-request) when the flow is launched directly from a web browser.

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Inputs salutation askGender promptRealName
```

The statements that make up the flow (body) come after the header and start at column 1, ie. aligned with the `Flow` keyword: 

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Inputs salutation askGender promptRealName

x = "Hi"
y = false
...
```

See the [full reference](./dsl-full.md#flow-structure) to learn more about flow structure.

## Logging

With the `Log` instruction data can be sent to the flows log. Check the [logging](./logging.md) page for more details. Some illustrative examples follow: 

|Code|Message appended|Notes|
|-|-|-|
|`Log "Hi there"`|Hi there||
|`Log "Hello" "world" 0 false`|Hello world 0 false|`Log` can be passed a variable number of parameters|
|`Log [1, 2, 3, 4, 5]`|1, 2, 3, ...more|Lists and maps are not traversed wholly|
|`Log "Hell%% 0 %" "o" " world" false`|Hello world 0 false|Placeholders usage|
|`Log "@warn Today is Friday %th" 13`|Today is Friday 13th|Message logged as warning|

By default messages are logged at the `INFO` level. This can be customized as in the last row.

## Conditionals and branching

`When` and `Otherwise` allow to write conditionals. With `and`, `or`, `is` and `is not`, logical (boolean) expressions can be built. Examples:

```
car = { brand: "Ford", color: null, model: 1963 }
When car.color is null
    car.color = "pink"
    ...
```

Nested conditionals:

```
... 
When car.color is "pink"
    When car.brand is "Ford"
        Log "Weird-looking car"
        ...
``` 

Use of `Otherwise`:

```
...
When car.color is not null
    Log "you have a regular painted car"
    ...
Otherwise
    ...
```

**Notes:**

- Equality is designed to work with `null`, numbers, strings, and boolean values only. More exactly, a number should only be compared to a number, a string to a string, etc., otherwise the equality test evaluates to false.
- Comparisons are limited to equality (`is`) or inequality (`is not`). For other forms of comparison you can resort to Java.

Agama also has `Match ... to`, a construct similar to C/Java `switch`.

See the [full reference](./dsl-full.md#conditionals-and-branching) to learn more about conditionals and branching.

## Flow finish

`Finish` is used to terminate a flow's execution. In general, a flow can finish successfully or failed. Examples:

<table>
	<tr><th>Code</th><th>Meaning</th></tr>
	<tr>
<td>

```
it = { success: true, data: { userId: "as9233Qz", ... }}
Finish it
```

</td>
		<td>Flow finished successfully. Some relevant data attached</td>
	</tr>
	<tr>
<td>

```
it = { success: false,
    error: "User entered a wrong password 3 times" }
Finish it
```

</td>
		<td>Flow failed. Error description attached</td>
	</tr>
	<tr>
<td>

```
Finish "as9233Qz"
```

</td>
		<td>Shorthand for <code>{ success: true, data: { userId: "as9233Qz" } }</code></td>
	</tr>
</table>

<!--
|Code|Meaning|
|-|-|
|`it = { success: true, data: { userId: "as9233Qz", ... }}`<br/>`Finish it`|Flow finished successfully. Some relevant data attached|
|`it = { success: false,`<br/>`        error: "User entered a wrong password 3 times" }`<br/>`Finish it`|Flow failed. Error description attached|
-->

See the [full reference](./dsl-full.md#flow-finish) to learn more about flows termination.

## Web interaction

### RFAC

The `RFAC` (Redirect and Fetch at callback) instruction abstracts the process of redirecting the user's browser to an external site and collect the data presented later at a designated callback URL. 

!!! Important
    Agama engine's callback URL is `https://<your-server>/jans-auth/fl/callback`.

Example:

```
result = RFAC "https://login.twitter.com/?blah..&boo=..."
```

This redirects to the given URL by issuing an HTTP redirect ("302 Found"). Once the user browser is taken to the callback by the external site (e.g. twitter.com), the data included in the query string or payload is stored in `result` (a _map_) for further processing. See the [full reference](./dsl-full.md#rfac) to learn more.

### RRF

The `RRF` (Render-Reply-Fetch) instruction expresses the concept of "show a page and grab user data after interaction". It takes the path to a template and injects a value into it. The produced (**R**endered) markup is sent (**R**eplied) to the browser and finally, the result of the interaction of the user with the page can be retrieved (**F**etched).

<table>
	<tr><th>Example</th><th>Details</th></tr>
	<tr>
<td>

```
RRF "survey.ftl"
```

</td>
		<td>Renders the template <code>survey.ftl</code> (located in this flow's base path) and resulting markup is replied to user's browser.<br/>Data submitted by the user is ignored
		</td>
	</tr>
	<tr>
<td>

```
obj = { salutation: "Hey!", ... }
result = RRF "survey.ftl" obj
```

</td>
		<td>Renders the template <code>survey.ftl</code> by injecting the data passed in <code>obj</code> and the resulting markup is replied to user's browser.<br/>Data submitted by the user is stored in variable <code>result</code>: a map whose keys are named according to the form fields present in <code>survey.ftl</code>
		</td>
	</tr>
</table>
<!--
|Example|Details|
|-|-|
|`RRF "survey.ftl"`|Renders the template `survey.ftl` (located in this flow's base path) and resulting markup is replied to user's browser.<br/>Data submitted by the user is ignored|
|`obj = { salutation: "Hey!", ... }`<br/>`result = RRF "survey.ftl" obj`|Renders the template `survey.ftl` by injecting the data passed in `obj` and the resulting markup is replied to user's browser.<br/>Data submitted by the user is stored in variable `result`: a map whose keys are named according to the form fields present in `survey.ftl`|
-->

**Notes:**

- The template location must be specified with a string literal only (not variables)
- `RRF` has more capabilities. See the [full reference](./dsl-full.md#rrf) to learn more.

## Looping

### Repeat

`Repeat` was designed with the concept of attempts/retries in mind: a set of statements are executed, a condition can
optionally be supplied in order to abort the loop early, and (optionally too) a block of statements can be executed before the next iteration is started if the condition evaluated to `false`. Example:

```
x = ... // an integer value
month = "..."
obj = { error: null }
y = Repeat x times max
    data = RRF "guess_birthday_month.ftl" obj
    Quit When data.guess is month
    obj.error = "Wrong! try again"
```

This loop runs `x` iterations at most. At every iteration the template `guess_birthday_month.ftl` is RRF'ed. If the value provided at the browser matches that of `month` variable the loop is aborted earlier, otherwise an error message is set - and template may potentially use it. After the loop finishes, variable `y` will contain the total number of iterations made to completion. This excludes partial iterations aborted by `Quit`.

**Notes:**
- `Quit` and the statements following are optional
- The variable assignment before the `Repeat` keyword is optional
- See the [full reference](./dsl-full.md#repeat) to learn more about `Repeat`

### Iterate over

`Iterate over` is used to traverse the items of a string, list, or the keys of a map. At every iteration, a variable is set with the current item or key name. As with `Repeat`, a loop may be aborted earlier, an optional block of statements can be specified after `Quit`, and the total number of iterations can be stored in a variable. Example:

```
seasons = [ "spring", "winter", "fall", "summer" ]
sports = [ "soccer," "golf", "tenis" ]
Iterate over seasons using sn
    y = Iterate over sports using sport
        Log "Shall we play % in % ?" sport sn
        Quit When sn is "winter"
        Log "yes!"
    Log "We played % sports in %" y sn
```

The above features loop nesting. The outer loop iterates over `seasons` list and the inner one over `sports`. The variables `sn` and `sport` hold the current visited element. The inner loop is aborted upon a given condition. The total number of complete iterations is recorded in `y` every time the inner loop finishes.

See the [full reference](./dsl-full.md#iterate-over) to learn more about `Iterate over`.

## Subflows

A flow can `Trigger` another flow (a.k.a subflow) and grab its response when `Finish`ed. This feature materializes flow composition and re-use in Agama. Example:

```
outcome = Trigger jo.jo.PersonalInfoGathering null false 
Log "subflow returned with success?" outcome.success
```

The above starts the flow identified by `jo.jo.PersonalInfoGathering` passing the given parameters (assuming it receives two inputs). When done, `outcome` will reference the map that was employed to `Finish` the subflow, in other words, its response.

See the [full reference](./dsl-full.md#subflows) to learn more about `Trigger`.

## Java interaction

Agama interfaces seemlessly with Java or Groovy by means of the `Call` instruction. 

Recall the DSL is designed to force developers use Java when the task at hand cannot be implemented by simple data manipulation or comparison of values. This way a flow written in Agama DSL serves fundamentally as a depiction of the flow itself, hiding most of the internal details and low-level computations.

See the [full reference](./dsl-full.md#java-interaction) to learn more about `Call`.
