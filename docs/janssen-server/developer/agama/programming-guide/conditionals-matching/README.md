# Conditionals and matching

This page illustrates the usage of [`When`/`Otherwise`](https://docs.jans.io/stable/agama/language-reference/#conditionals-and-branching) in Agama. They are equivalent to `if`/`else` in other programming languages.

## When and how

To write a conditionals, `When` must be followed by a logical expression. This kind of expression consists of one or more equality comparisons chained by disjunction or conjunction operators. In Agama, there are only two equality comparison operators: `is` and `is not` (equivalent to `==` and `!=` in `C` language). The disjunction and conjunction operators are `or` and `and` respectively (equivalent to `||` and `&&`).

The block of code to execute in case the logical expression evaluates to `true` must be indented under `When`. Here is an example:

```
lang = "Agama"
day = "Monday"
When lang.length is 5 or day is "Tuesday"
    Log "Aha!"
    RRF "page.htm"
```

In this case, the expression is truthy because the first comparison evaluates to `true` (the length of variable `s` is five). The second comparison is not even evaluated due to the short-circuit nature of the `or` operator.

Absence of parenthesized expressions in Agama implies there is no direct support for grouping sub-expressions in large logical expressions. However, in practice most conditionals needed in flows are fairly simple. More sophisticated expressions can be written by reorganizing its components in a way that operator precedence helps take the role of parenthesis.

As an example suppose something like `(!damp || cold) && cloudy` is required. This may be re-written as `!damp && cloudy || cold && cloudy` (`&&` takes precedence over `||`). Thus, in Agama it would be coded like:

```
When damp is false and cloudy is true or cold is true and cloudy is true
```

Too verbose!, however developers will rarely have to resort to things like this.

**Notes**: 

- Comparisons require a value to compare against explicitly, so `cold and cloudy` is not a valid expression. The correct version would be `cold is true and cloudy is true`
- Likewise, `not damp` is invalid. Use `damp is false` instead
- `not` alone is **not** an Agama keyword. `is not` actually is, however it is used for testing inequality  

## Example: the unforgiving club

Suppose a ficticious club exists in which becoming a member requires filling out a form online which is then evaluated by a strict board of former members. Aspiring members are required to supply their name, e-mail, and choose from a set of personal interests to develop in the club. The board members are picky and refuse to study any application coming without personal interests selected or with too much of them.

The first version of this flow is as follows:

```
Flow com.acme.basic.club_application1
    Basepath ""

data = { interests: [ "Coding", "Music", "Dancing", "Cooking" ] }
regData = RRF "application.ftlh" data

// Not enough or too much interests selected?
When regData.interest is null
    data = { msg: "You have no interest at all!" }
    RRF "rejected.ftlh" data
    Finish false
Otherwise
    When regData.interest.length is 4
        data = { msg: "Seems you have too much interests..." }
        RRF "rejected.ftlh" data
        Finish false
    Otherwise
        Finish true
```

### Application template

Template [`application.ftlh`](./project/web/application.ftlh) has a form to capture name and e-mail. It also generates checkboxes dynamically for the interests which are passed in the *map* parameter `RRF` receives. This is done as follows:

```
<#list interests as val>            
    <input type="checkbox" id="interest_${val?counter}" name="interest" value="${val}" />
    <label for="interest_${val?counter}">${val}</label>
</#list>
```

This code traverses the `interests` list originally in `data`. At each iteration the (Freemarker) variable `val` is bound to the current element of the list. The `?counter` next to `val` is a ["loop variable built-in"](https://freemarker.apache.org/docs/ref_builtins_loop_var.html) that returns the one-based index of the current iteration. This is how the generated HTML looks like:

```html
<input type="checkbox" id="interest_1" name="interest" value="Coding" />
<label for="interest_1">Coding</label>

<input type="checkbox" id="interest_2" name="interest" value="Music" />
<label for="interest_2">Music</label>

etcetera...
```

**Note**: Freemarker variables cannot be accessed or manipulated in Agama flows. On the other hand, data injected into templates via `RRF` (the "data-model" in Freemarker jargon) cannot be modified in Freemarker. See ["Defining variables in the template"](https://freemarker.apache.org/docs/dgui_misc_var.html).

### Flow code disection

Once the form is submitted, data supplied is bound to variable `regData`. This is a *map* which will have keys `name` and `email`. If one or more interests were selected, the key `interest` will exist and contain a *list* of *string*s.  

The first `When` in `com.acme.basic.club_application1` checks if no interests were selected, that is, if `regData` has no key named `interest` (note this is the name of the checkbox fields in the form). In this case, a rejection page is shown (more on it later) and the flow finishes with failure.

If the logical expression evaluates `false` (`interest` exists), code execution continues at the `Otherwise` block where another `When` is found. The check there is regarding the length of the list `interest` in `regData`. If the list length is four, the rejection page is displayed as well and the flow finishes with failure. 

In case the list length length is not four, the flow finishes successfully; no UI feedback shown to the user.  

### Rejection template

Template [`rejected.ftlh`](./project/web/rejected.ftlh) is self explanatory. It just shows a message upfront and an "OK" button to proceed. The message displayed is whatever was passed to `RRF` (key `msg`).  

### Flow analysis

`com.acme.basic.club_application1` illustrates well the use of `When`/`Otherwise`, however it has unnecessary nesting of blocks. If the `Finish` inside the first `When` is hit, that would be the last instruction executed in the flow, so having an `Otherwise` is actually not needed. The same applies to the second `When`. The less nesting, the shorter and easier-to-understand code.

The flow code can also be shortened if the rejection page shows a generic error message instead of specifying what went specifically wrong. The below is an improved, more compact version of the flow:

```
Flow com.acme.basic.club_application2
    Basepath ""

data = { interests: [ "Coding", "Music", "Dancing", "Cooking" ] }
regData = RRF "application.ftlh" data

list = regData.interest
// Not enough or too much interests selected?
When list is null or list.length is 4
    RRF "rejected2.ftlh"
    Finish false

Finish true
```

Here [`rejected2.ftlh`](./project/web/rejected2.ftlh) is a pure HTML page with a static message.

## FAQ

### Do comparisons work with literals only?

No. The operands for a comparison can be literals, variables, or similar things. The following are all valid logical expressions: `false is false`, `4 is list.length`, `list.length is myNumber`, `list.length is not myMap.myKey`.

### How to do numerical comparison?

Agama does not support things like "less than" or "greater than". This may require usage of a foreign language call but real-world experience shows this type of computations do not come up when writing flow structure.

### Is there an `elsif` equivalent in Agama?

No.

### Is there a `switch` equivalent?

Yes. This is done via [`Match`](https://docs.jans.io/stable/agama/language-reference/#advanced-matching).
