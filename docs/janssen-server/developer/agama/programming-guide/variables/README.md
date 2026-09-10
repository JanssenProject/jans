# A closer look to values and variables

Handling values and variables is key to grasp any language. The flows shown in this page exemplify how this works in Agama. They should feel pretty natural to any programmer.

**Note**: As in most languages, code may contain comments. Use `//` to start a comment. Comments in Agama only span one line.

## Literals

Flow `com.acme.basic.literals` contains examples of literal values in Agama. Visit [Literals](https://docs.jans.io/stable/agama/language-reference/#literals) in the language reference for a deeper insight.

```
Flow com.acme.basic.literals
    Basepath ""
    
b = true            // A boolean value
str = "Oh, Agama!"  // A string
                  
integer = -10       // A signed number
real = 3.1416       // A number with decimal digits
                  
surprise = null     // Interfacing with foreign languages makes null a need...

l = [ false, "Aga", 2.7183, null ]    // Lists are not "typed"

car = { brand: "Ford", color: null, model: 1963, overhaulsIn: [ 1979, 1999 ] }  // This is a map  

RRF "car_details.ftlh" car

Finish true
```

Template `car_details.ftlh` displays some data passed in variable `car`. Here is how it looks like:


```
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
    <h1>My Car</h1>
    <dl>
        <dt>Brand</dt>
        <dd>${brand}</dd>
        
        <dt>Model</dt>
        <dd>${model?c}</dd>
        
        <dt>Overhauls:</dt>
        <dd>
            <#list overhaulsIn>
                <ul>
                    <#items as year>
                        <li>${year?c}</li>
                    </#items>
                </ul>
            <#else>
                None yet
            </#list>
        </dd>        
    </dl>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="submit" value="Finish">
    </form>
</body>
</html>
```

Here, `brand` and `model` from `car` variable are shown. The `overhaulsIn` list is traversed using Freemarker's [list directive](https://freemarker.apache.org/docs/ref_directive_list.html) to generate an HTML bulleted list. Usage of `?c` avoids formatting numbers using locale settings. Without it, 1963 would be printed as 1,963. More on `c` [here](https://freemarker.apache.org/docs/ref_builtins_number.html#ref_builtin_c).

## Variables

Flow `com.acme.basic.variables` contains examples of variables manipulation in Agama. In the language reference visit [Accessing and mutating data in variables](https://docs.jans.io/stable/agama/language-reference/#accessing-and-mutating-data-in-variables) and [Advanced cases in variable manipulation](https://docs.jans.io/stable/agama/language-reference/#advanced-and-special-cases-in-variable-manipulation)  for a deeper insight.

```
Flow com.acme.basic.variables
    Basepath ""
    
s = "Agama"           // A string
len = s.length        // String length (equals to 5)

letter = s[0]         // A string (equals to "A")
len = letter.length   // String length (equals to 1)

s[1] = "N"            // Strings are immutable. s is still "Agama"

l = [ true, false , null ]  // A list
len = l.length              // List length (equals to 3)
b = l[0]                    // A boolean (equals to true)
 
l[1] = true          // Replaces the former false value
nothing = l[4]       // This does not crash (equals to null)
l[3] = false         // Expands the list. It is now [ true, true, null, false]

car = { }            // Empty map
car.brand = "Ford"   // Attaches a new key/value pair
owner = car.owner    // Unknown key accesss (equals to null)

car.brand = "Simca"  // Overwrite the brand value
car.model = 1934     // car is now { brand: "Simca", model: 1934 }

key = "brand"  
s = car.$key         // Map access using dynamic key name (evaluates "Simca")
s = car."brand"      // Evaluates "Simca"

car.size = { H: 1.4, W: 1.2, L: 2.3 }
//car is now { brand: "Simca", model: 1934, size: { H: 1.4, W: 1.2, L: 2.3 } }
car.overhaulsIn = [ ]
height = car.size.H     // evaluates 1.4

RRF "car_details.ftlh" car

it = { success: true }
Finish it
```

As in the former example, `brand` and `model` from `car` are shown. Here, "None yet" will be displayed in the "Overhauls" section.

Note this flow terminates a bit differently compared to others; it supplies a *map* variable. So far all examples have used the "shortcut" notation for [`Finish`](https://docs.jans.io/stable/agama/language-reference/#flow-finish). Here an "expanded" form is employed. This is often used when extra information needs to be passed in the `Finish` statement, however in this case, it has the same effect of doing `Finish true`.
