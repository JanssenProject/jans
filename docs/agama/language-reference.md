---
tags:
  - developer
  - agama
---

# Language reference

## Agama DSL reference

This document surveys the different constructs of the Agama domain-specific language.

## Code comments

There is support for single line comments only (no block comments). Use `//` to start a comment. Comments can start anywhere in a line.

## Data types

In practice, values would fit into any of: _string_, _boolean_, _number_, _list_ or _map_. Since routines implemented in other languages can be invoked (more on this later), returned values might not match up exactly with these categories, however this is not too relevant because there is no strict type enforcement in Agama.

## Literals
 
### Strings

- They are surrounded by double quotes. Examples: `"Agama"`, `"blah"`, `""` (empty string) 

- Backslash can be used to escape chars, like `"Hello\nGluu"` (line feed), `"Hi\u0040"` (unicode character) 

- Including double quotes in strings requires unicode escaping, like `"\u0022"`. Using `"\""` won't work  

### Booleans

- Only `true` or `false` allowed (notice they are lowercased) 

### Numbers

- They are expressed in base 10 only 

- Can be signed or unsigned, with or without decimal: `0`, `-1`, `2.0`, `2.3`, `-3.000001`, etc. 

- No exponential notation allowed (e.g. `1E-05`) 

- The following are not valid: `.1`, `-.1`, `+1`. These are their OK equivalents: `0.1`, `-0.1`, `1` 

### Null

The “special” value `null` can be used (responsibly) to represent the absence of a value. It is a direct consequence of supporting the integration of other languages in the DSL.

### Lists

- They are finite sequences. Elements are separated by commas 

- Examples: `[ 1, 2, 3 ]`, `[ "bah!", "humbug" ]`, `[ ]` (empty list) 

- Elements of a list do not have to be of the same type: `[ false, [ 0, 1], "?" ]` is legal but generally discouraged 

- Commas can be surrounded by any combination of spaces and new lines. This is handy when a list takes up some space. This is legal: 

```
[ "no", "such",  "thing"  
, "as", "a",
            "stupid","question"]
```

### Maps

- They are in essence associative arrays (a.k.a. dictionaries): unordered collections of key/value pairs 

- Example: `{ brand: "Ford", color: null, model: 1963, overhaulsIn: [ 1979, 1999 ] }`. This map keys are `brand`, `color`, `model`, and `overhaulsIn` 

- In literal notation, keys names must follow the pattern `[a-zA-Z]( _ | [a-zA-Z] | [0-9] )*` so these are all valid key names: `a`, `Agama`, `b_a`, `a0_0`; on the contrary, `_a`, `9`, `-a`, and `"aha"` are invalid key names 

- As with lists, commas can be surrounded by any combination of spaces and new lines

## Variables

- Variable names follow the pattern: `[a-zA-Z]( _ | [a-zA-Z] | [0-9] )*`  

- _camelCase_ naming is recommended 

- Variables are not declared, just used freely. Variables are always global in a given flow  

- They can be assigned a value using the equal sign. Example: `colors = [ "red", "blue" ]`   

- They can be assigned several times in the same flow


## Accessing and mutating data in variables

### Strings  

- Suppose `x` is a string. Individual characters can be accessed by zero-based indexes: `x[0]`, `x[1]`, etc. and they are themselves considered strings of size 1 

- `x.length` returns the string size (number of characters in it). 

- Strings are not modifiable (neither size nor individual characters can be altered) 

### Lists  

- Suppose `x` is a list. Elements can be accessed by zero-based indexes: `x[0]`, `x[1]`, etc. 

- Elements of a list can be assigned (and re-assigned) using indexes too. Example: `x[2] = false` 

- `x.length` returns the list size. This value can be updated in order to shrink or grow a list (e.g. `x.length = 10`). When extending a list beyond its current length, the “gap” created is filled with `null` values  

- An attempt to access an index position greater than or equal to the list size returns `null` (most general-purpose languages would raise a runtime error in this situation) 

- Using expressions for indexing is **not** allowed, like `x[person.age]`, `x[y[0]]` 

- Click [here](#advanced-and-special-cases-in-variable-manipulation) to learn more about access in lists 

### Maps  

- Suppose `x` is map. Values can be accessed by using “dot notation” 

- Say `x = { brand: "Ford", color: null, model: 1963, overhaulsIn: [ 1979, 1999 ] }`, then `x.model` evaluates to `1963`, and `x.overhaulsIn[1]` evaluates to `1999` 

- Setting the color would be like `x.color = "white"` 

- A new key/value pair can be appended too: `x.maxSpeed = 90` 

- Access of an unknown property evaluates to `null`: `x.owner` 

- If a key name does not follow the pattern `[a-zA-Z]( _ | [a-zA-Z] | [0-9] )*` an alternative notation must be employed to retrieve or modify the associated value. Click <a href="#maps-and-dot-notation">here</a> to learn more 

## Flow structure

A flow written in Agama consists of a header and one or more statements following.

### Header basics

A header is declarative by nature. It starts with the `Flow` keyword followed by the qualified name of the flow, e.g. `Flow com.acme.FoodSurvey`. A qualified name is a sequence of one or more fragments separated by dot (`.`) where a fragment follows the pattern `[a-zA-Z]( _ | [a-zA-Z] | [0-9] )*`. By convention, qualified names shall adhere to a reverse Internet domain name notation.

In a header, at minimum, a base path literal string must be provided (in an indented block). This sets the location where flow assets will reside relative to the assets root:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
```

Here, **assets** refer to all elements required to build the end-user experience: UI pages, stylesheets, images, etc. The storage of these elements and location of the "root" are specific to the concrete Agama [engine implementation](./execution-rules.md#assets-management) used. Generally it will resemble a directory structure in a filesystem.

Next, flow timeout may be specified. This is the maximum amount of time the end-user can take to fully complete a flow. For instance:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Timeout 100 seconds
```

An unsigned integer literal value must be specified after the `Timeout` keyword, if present.

The `Configs` keyword may be used to designate a variable so the flow's properties can be accessed in the code. These properties are usually provided when the flow is created - normally through an administrative tool. This process varies from engine to engine. 

Often flow properties are used to supply configuration parameters to the flow. As an example, suppose a flow that sends a confirmation e-mail has to be implemented. Properties are a good place to hold the configuration of the outgoing mail server to employ: name, port, authentication credentials, etc. For instance, this is how the configuration properties would be bound to variable `conf`:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Timeout 100 seconds
    Configs conf
```

### Inputs

In addition to the above, and optionally too, flows may receive inputs from their callers. Input names can be listed using the `Inputs` keyword:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Inputs salutation askGender promptRealName
```

Input names follow the same naming conventions (patterns) of variables and can be treated as such in code.

!!! Important
    Note the difference between properties and inputs. Properties are parameters that callers of the flow should not control or be interested in. On the other hand, inputs are parameters that callers supply explicitly to make the flow exhibit certain behaviors.

Check this [section](#input-parameters) to learn how callers can pass values to input parameters of a flow.

### Flow statements

The statements that make up a flow - body - come after the header and start at column 1, ie. aligned with the `Flow` keyword: 

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Timeout 100 seconds
    Configs conf
    Inputs salutation askGender promptRealName

x = "Hi"
y = false
...
```

There are several types of statements: branching, looping, web interaction, etc. They will be regarded in the subsequent sections of this document. Note Agama does not support the concept of subroutines/procedures; this is not needed because functional decomposition is carried out by calling [subflows](#subflows).

## Logging

Flows can issue small messages (normally used as a form of troubleshooting) that will be appended to a log. Every message can be associated a _severity_ level. Both the log location and available levels are engine specific.

To append data to the flows log, use the `Log` instruction. Examples:

|Code|Message appended|Notes|
|-|-|-|
|`Log "Hi there"`|Hi there||
|`Log "Hello" "world"`|Hello world|`Log` can be passed a variable number of parameters|
|`Log "Hello" "world" 0 false`|Hello world 0 false||
|`Log [1, 2, 3, 4, 5]`|1, 2, 3, ...more|Lists and maps are not traversed wholly|
|`Log "Hell%% 0 %" "o" " world" false`|Hello world 0 false|Placeholders usage|
|`Log "% % % yes" 1 "two"`|1 two % yes||
|`Log "3" "%" 0`|3 % 0||
|`Log "@warn Today is Friday %th" 13`|Today is Friday 13th|Message logged as warning (if the engine features a "warning" level)|
|`Log "@w Today's Armageddon \u263A"`|Today's Armageddon ☺|Message logged as warning (if the engine features a "w" level - shortcut method)|

Check your engine's documentation to learn more about how statements are logged.

## Conditionals and branching

Keywords `When` and `Otherwise` allow to write conditionals. With `and`, `or`, `is`, and `is not`, logical (boolean) expressions can be built. Examples:

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

Boolean expressions can span several lines: `and` and `or` can be at the start or end of a line as long as the whole expression is left-aligned with its corresponding `When`. Examples:

```
//legal:

When day is cloudy
    When there is hope and
    there is mercy  
        Log "let's rock n' roll"

    ...

When day is cloudy
    When there is hope and
        // :D
    there is mercy
    or fear is null
        Log "let's rock n' roll"

    ... 
```


```
//illegal:

When day is cloudy
    When there is hope and
        // :D
    there is mercy  
        or fear is null
        Log "let's rock n' roll"   

    ...

When day is cloudy
    When there is hope and
        // :D
    there is mercy
or fear is null
        Log "let's rock n' roll"   

    ...

```

**Notes:**

- Equality is designed to work with `null`, numbers, strings, and boolean values only. More exactly, a number should only be compared to a number, a string to a string, etc., otherwise the equality test evaluates to `false`. Comparing a value with itself evaluates to true regardless of type, i.e. `car is car`, `null is null`, `false is false` are all truthy

- Comparisons are limited to equality (`is`) or inequality (`is not`). For other forms of comparison you can resort [foreign routines](#foreign-routines)

- As expected `and` has higher priority than `or` when evaluating expressions. There is no way to group expressions to override the precedence: there are no parenthesis in Agama. Assigning the result of a boolean expression to a variable is not supported. These restrictions are important when writing conditionals

### Advanced matching

Agama's `Match ... to` is a construct similar to C/Java `switch`. Example:

```
car = ...
x = ...
y = ...
// Assume x and y hold numbers
z = [ 3.1416, 2.71828 ]

Match car.model to
   x
      //Code in this block will be executed if car.model is equal to the value of x
      ...

   -y //Here we use minus y
      ...  

   z[0]
      ...

   1.618 //Literal values can be used too for matching
      ...  

   null
      ...

Otherwise    //optional block
   //Instructions here are executed if there was no match at all 
```

## Flow finish

`Finish` is used to terminate a flow's execution. A flow can finish successfully or failed. Examples:

<table>
	<tr><th>Code</th><th>Meaning</th></tr>
	<tr>
<td>

```
Finish true
```

</td>
		<td>Shorthand for flow finished successfully</td>
	</tr>
	<tr>
<td>

```
Finish false
```

</td>
		<td>Shorthand for failed flow</td>
	</tr>
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
	<tr>
<td>

```
it = { nonsense: [ null ] }
Finish it.nonsense
```

</td>
		<td>This causes the flow to crash. Note this is not equivalent to <code>Finish false</code> (which means the flow ended with a negative outcome).</td>
	</tr>
</table>

**Notes:**

- Unless otherwise stated by the concrete engine implementation, a _map_ literal should not be passed directly as argument. This means the following is illegal: `Finish { success: false, error: "spacetime singularity" }`. The examples above list several syntactically valid usages
- Any statement found after `Finish` is not reached and thus, not executed
- If no `Finish` statement is found in a flow's execution, this will degenerate in flow [crash](./execution-rules.md#crashed-flows)
- When a flow is finished and was used as [subflow](#subflows) (part of the execution of a bigger parent flow), the parent does not terminate. Execution continues at the following instruction that triggered the subflow. More on `Trigger` later
- Using `data` in the `Finish` directive is an effective way to communicate information to callers (parent flows)
- Learn more about flows lifecycle [here](./execution-rules.md#flows-lifecycle) 

## Web interaction

Web interaction constructs bring the most value to Agama. Developers can express the concepts of ”redirect a user to an external site and retrieve any data later provided” or “show a page and grab user data after interaction” using atomic instructions.

### RFAC

`RFAC` (stands for _Redirect and Fetch at callback_) abstracts the process of redirecting the user's browser to an external site and then collect the data presented later at a designated callback URL. This feature is particularly useful in inbound identity scenarios (e.g. to support social login).

<table>
	<tr><th>Example</th><th>Details</th></tr>
	<tr>
<td>

```
RFAC "https://login.twitter.com/?blah..&boo=..."
```

</td>
		<td>Redirects to the given location. Once the user browser is taken to the callback URL by the external site (twitter.com), the flow continues ignoring any data included</td>
	</tr>
	<tr>
<td>

```
map = { twitter: { loginUrl: "https://...", ... }, ... }
result = RFAC map.twitter.loginUrl
```

</td>
		<td>Redirects to the given location. Once the user browser is taken to the callback URL by the external site, the data included in the query string or payload is stored in <code>result</code> (a map) for further processing</td>
	</tr>
</table>

The callback URL varies depending on the engine used. Check your engine's docs.

### RRF

`RRF` (stands for _Render-Reply-Fetch_) abstracts the process of rendering a UI template, send the produced markup to the browser and grab user-provided data back at the server side.

<table>
	<tr><th>Example</th><th>Details</th></tr>
	<tr>
<td>

```
RRF "survey.htm"
```

</td>
		<td>Renders the template <code>survey.htm</code> (located in this flow's base path) and resulting markup is replied to user's browser.<br/>Data submitted by the user is ignored
		</td>
	</tr>
	<tr>
<td>

```
obj = { salutation: "Hey!", ... }
result = RRF "survey.htm" obj
```

</td>
		<td>Renders the template <code>survey.htm</code> by injecting the data passed in <code>obj</code> and the resulting markup is replied to user's browser.<br/>Data submitted by the user is stored in variable <code>result</code>: a map whose keys are named according to the form fields present in <code>survey.htm</code>
		</td>
	</tr>
</table>

**Notes:**

- The template location must be specified with a string literal only (not a variable)
- Where and how to store templates is an engine-specific detail as well as the file formats supported. See [Assets management](./execution-rules.md#assets-management).
- Use _map_ variables - not literal _maps_ - for the second argument of `RRF`

## Looping

There are two constructs available for looping in Agama: `Repeat` and `Iterate over`.

### Repeat

`Repeat` was designed with the concept of attempts/retries in mind: a set of statements are executed, a condition can optionally be supplied in order to abort the loop early, and (optionally too) a block of statements can be executed before the next iteration is started if the condition evaluated to `false`. A loop is given a maximum number of iterations. Examples:

<table>
	<tr><th>Example</th><th>Notes</th></tr>
	<tr>
<td>

```
month = "…"
Repeat 3 times max
    data = RRF "guess_birthday_month.htm"
    //Quit is optional in loops
    Quit When data.guess is month 
```

</td>
		<td>A loop that runs 3 iterations at most.<br/>A page is shown at every iteration.<br/>If the value entered by the user matches that of <code>month</code> variable, the loop is aborted earlier</td>
	</tr>
	<tr>
<td>

```
x = … // an integer value
month = "…"
obj = { error: null }
Repeat x times max
    data = RRF "guess_birthday_month.htm" obj
    Quit When data.guess is month
    obj.error = "Wrong! try again"
```

</td>
		<td>Similar to previous example<br/>This time the max no. of iterations is set using a variable<br/>When there is a miss a message error is set (which the UI template may potentially use)</td>
	</tr>
	<tr>
<td>

```
x = … // an integer value
month = "…"
obj = { error: null }
y = Repeat x times max
    data = RRF "guess_birthday_month.htm" obj
    Quit When data.guess is month
    obj.error = "Wrong! try again"
    Log "Attempt number:" idx[0]
```

</td>
		<td>Similar to previous example<br/>After the loop finishes, variable <code>y</code> will contain the total number of iterations made to completion. This excludes partial iterations aborted by <code>Quit</code>, thus, <code>y <= x</code><br/>Note the usage of implicit variable <code>idx</code> which holds the current (zero-based) iteration number</td>
	</tr>
</table>

### Iterate over

`Iterate over` is used to traverse the items of a string, list, or the keys of a map. At every iteration, a variable is set with the current item or key name. As with `Repeat`, a loop may be aborted earlier, an optional block of statements can be specified after `Quit`, and the total number of iterations can be stored in a variable.

<table>
	<tr><th>Example</th><th>Notes</th></tr>
	<tr>
<td>

```
seasons = [ "spring", "winter", "fall", "summer" ]
Iterate over seasons using sn
    Log "There is nothing like" sn
```

</td>
		<td>A loop running over a simple list. Every element visited is referenced with variable <code>sn</code></td>
	</tr>
	<tr>
<td>

```
human = { weight: 100, height: 5.9, age: 26 }
Iterate over human using attribute
    Log attribute "is" human.$attribute
```

</td>
		<td>Iterates over the keys of the map printing both the key and its associated value. To learn about the <code>.$</code> notation see <a href="#maps-and-dot-notation">Maps and dot notation</a></td>
	</tr>
	<tr>
<td>

```
seasons = [ "spring", "winter", "fall", "summer" ]
sports = [ "soccer", "golf", "tennis" ]
Iterate over seasons using sn
    Iterate over sports using sport
        Log "There is nothing like playing % in %" sport sn 
```

</td>
		<td>Nested loops</td>
	</tr>
	<tr>
<td>

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

</td>
		<td>Similar to the previous example. The inner loop is aborted upon a given condition. Note the total number of complete iterations is recorded in <code>y</code> every time the inner loop finishes.</td>
	</tr>
	<tr>
<td>

```
seasons = [ "spring", "winter", "fall", "summer" ]
sports = [ "soccer," "golf", "tenis" ]
Iterate over seasons using sn
    Iterate over sports using sport
        Log idx[0] idx[1] 
```

</td>
		<td>Prints iteration numbers: 0 0, 0 1, 0 2, 1 0, 1 1, 1 2, ... 3 2<br/>The index used in <code>idx</code> is <code>0</code> for the outermost loop and increases by one at every level of loop nesting</td>
	</tr>
</table>


## Subflows

A flow can `Trigger` another flow (a.k.a subflow) and grab its response when `Finish`ed. This feature materializes flow composition and re-use in Agama.

<table>
	<tr><th>Example</th><th>Notes</th></tr>
	<tr>
<td>

```
Trigger jo.jo.PersonalInfoGathering
```

</td>
		<td>Starts the flow with qualified name <code>jo.jo.PersonalInfoGathering</code>.<br/>Returned data is ignored</td>
	</tr>
	<tr>
<td>

```
outcome = Trigger jo.jo.PersonalInfoGathering null false
Log "subflow returned with success?" outcome.success
```

</td>
		<td>Starts a flow passing parameters (assuming <code>PersonalInfoGathering</code> receives two inputs).<br/><code>outcome</code> will contain the map used when the subflow ended</td>
	</tr>
	<tr>
<td>

```
outcome | E = Trigger jo.jo.PersonalInfoGathering null false 
```

</td>
		<td>Similar to the previous example. If for some reason <code>PersonalInfoGathering</code> crashes, variable <code>E</code> will hold a reference to the error for further processing. Otherwise <code>E</code> evaluates to <code>null</code>. The type/structure of <code>E</code> is an engine-specific detail. The variable on the left of the pipe (<code>|</code>) can be omitted if the outcome of the flow will not be inspected</td>
	</tr>
	<tr>
<td>

```
userPrefs = { otp: "...", ... }
Match userPrefs.otp to
    "e-mail"
        flow = "co.acme.EmailOTP"
    "sms"
        flow = "co.acme.SmsOTP"
Trigger $flow 
```

</td>
		<td>Starts a flow whose qualified name is determined at runtime</td>
	</tr>	
</table> 

### Input parameters

The values passed after the flow name in `Trigger` are supplied as input parameters in the order declared by the subflow's `Inputs`. When not enough values are passed, the unassigned inputs will hold a `null` value.

Unless otherwise stated by the concrete engine implementation, _list_ and _map_ literals should not be passed as arguments to `Trigger`:

- Illegal: `Trigger subflow { key: [ 1, 2 , 3] } [ "Yeeha!" ]`

- Legal: `Trigger subflow x car.model list[1] null false -3 "Sam"`

### Template overrides

When re-using flows, existing templates may not match the required look-and-feel or layout of the flow that is being built, or may require minor adjustments to fit better the parent's needs. These can be overcome by declaring which templates the developer would like to override for a given subflow call. Example:

```
outcome = Trigger jo.jo.PersonalInfoGathering
    Override templates "path/basic.htm" "" "path2/dir/detail.htm" "tmp/mydetail.htm"
Log "subflow returned with success?" outcome.success
```

In an indented block using the `Override templates` keyword, several string literals can be provided. They specify the paths of the (subflows) templates that will be overriden by the parent and the corresponding new paths. In the example above, templates `path/basic.htm` and `path2/dir/detail.htm` rendered by flow `PersonalInfoGathering` (or its subflows) won't be picked from these locations but from the base path of the current (parent) flow using names `basic.htm` and `tmp/mydetail.htm` respectively. Usage of empty string denotes reusing the same file name than the original file.

Alternatively, every pair of original vs. new path can be specified in a single line for more clarity, like this:

```
outcome = Trigger jo.jo.PersonalInfoGathering
    Override templates "path/basic.htm" "" 
    "path2/dir/detail.htm" "tmp/mydetail.htm"
    ...
```

!!! Note
    The new (overriding) templates will be injected with the same data original templates would receive. In other words, this directive only "modifies" the first parameter of `RRF` instructions.  

## Foreign routines

Business logic implemented in languages other than Agama can be re-used by means of the `Call` instruction. `Call` plays a key role because Agama code serves fundamentally as a depiction of a flow hiding most of the internal details and low-level computations which are in turn delegated to foreign routines.

The syntax of this directive is very general and actual semantics are left to the concrete engine. Roughly, here is how a `Call` can be structured:

```
( var_expr? ('|' var_name)? = )? Call alnum(.alnum)* (#alnum)? arg*
```

where:

- *var_name* is a [variable name](#variables), e.g.: `x`, `fooBar`, `x_0`
- *var_expr* is a variable expression, e.g.: `x`, `x.a`, `x[1].b`, etc.
- *alnum* is syntactically identical to *var_name* but does not necessarily refer to an existing variable in the code, for instance, it may describe a path to locate a routine in a library
- *arg* is a *var_expr* or any _string_, _number_, _boolean_, _null_ literal

So the below are all syntactically valid invocations:

```
Call apple#pear
Call apple#pear "Hi" x 16
Call apple.banana#pear "Hi" abc 16
foo = Call apple.banana#pear "Hi" abc 16
foo | error = Call apple.banana#pear "Hi" abc 16
| error = Call apple.banana#pear "Hi" abc 16 x[0].ahoy.ahoy
foo = Call street fighters true false
```

Again, the semantics are given by the specific engine. Consult your engine documentation for examples.

Unless otherwise stated by the concrete engine implementation, _list_ and _map_ literals should not be passed as arguments to method calls directly. This means the following is illegal: `Call co.Utils#routine { key: [ 1, 2 , 3] } [ "Yeeha!" ]`.

## Advanced and special cases in variable manipulation

### Indexing in lists

Accessing/modifying list elements requires providing a numeric index between the brackets, e.g. `x[ 0 ]`. Note variables can also be used for indexing, like `x[ y ]` where `y` is a positive integer or zero.

For the below table, assume `x = [ "one", "two", "three" ]`.

<table>
	<tr>
<td>

```
x[1]
//"two"
```

</td>
<td>

```
y = 1
x[y]
//"two"
```

</td>
<td>

```
x["1"]
//illegal
```

</td>
</tr><tr>
<td colspan="3">

```
x[ z[0] ]
//illegal: variable expressions not allowed for indexes 
```

</td>
</tr><tr>
<td colspan="3">

```
x[obj.property]
//illegal: variable expressions not allowed for indexes
```

</td>
	</tr>
</table>

### Maps and dot notation

The regular “dot” notation is limited in the sense it is fairly static: developers have to have prior knowledge about the keys' names, in other words,  about the structure of maps and nested submaps, like in `person.homeAddress.postalCode`.

Also, there might be cases where a key name does not fit the required pattern, like in `person.street-address` or `persona.dirección`; even worse, there might be cases where the actual key is only known at runtime.

There are ways to overcome this:

<table>
	<tr><th>Example</th><th>Notes</th></tr>
	<tr>
<td>

```
x."- wow!"
```

</td>
		<td>Access the value associated to the key named <code>-wow!</code></td>
	</tr>
	<tr>
<td>

```
prop = ...
x.$prop 
```

</td>
		<td>Access the value associated to the key whose name is contained in the variable <code>prop</code> (that holds a string value). Note actual value of <code>prop</code> may be originated from a Java call or another form of computation</td>
	</tr>
	<tr>
<td>

```
propA = ...
propB = ...
x.$propA.c."d".$propB
```

</td>
		<td>A mix of notations is valid.<br/>For example, if <code>x= { a: { b: 0, c: { c: true, d: { e: null, f: "hello" } } } }</code>, <code>propA</code> is equal to <code>"a"</code>, and <code>propB</code> to <code>"f"</code>, the expression on the left evaluates <code>"hello"</code></td>
	</tr>
</table>

Usage of `.$` requires to supply a variable after the dollar sign: grouped variable expressions are not supported. Thus, it is not possible to achieve something like `x.a.c.($map.mykey).f` in order to obtain `"hello"` if `map = { mykey: "d" }`.

### Indexing in maps

For convenience, when a key name “looks like” a positive integer (or zero), e.g. `"2"`, `"10"`, etc., numeric values can directly be used to get/set data in a map:

```
x = { }
x."1" = "golf"
x[1]    //retrieves "golf"
x[2] = "polo"    //adds the key/value pair "2" / "polo"
```

## Language keywords

The following is a list of reserved words and as such, cannot be used as variable names or maps keys (in literal notation).

|Keyword|Purpose/usage|
|-|-|
|Basepath|header declaration|
|Call|Java interaction|
|Configs|header declaration|
|Finish|termination|
|Flow|header declaration|
|Inputs|header declaration|
|Iterate over|loops|
|Log|logging|
|Match|conditionals|
|Otherwise|conditionals|
|Override templates|web interaction|
|Quit|conditionals and loops|
|Repeat|loops|
|RFAC|web interaction|
|RRF|web interaction|
|seconds|header declaration|
|times max|loops|
|to|conditionals|
|Timeout|header declaration|
|Trigger|subflow calls|
|using|loops|
|When|conditionals|

|Operator|
|-|
|and|
|is|
|is not|
|or|

|Special literals|
|-|
|true|
|false|
|null|
