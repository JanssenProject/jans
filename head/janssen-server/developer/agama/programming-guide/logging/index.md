# Logging: a `printf` equivalent

`printf`-style instructions are present in most, if not all programming languages. Agama is not the exception, however it deviates a bit from the canon in favor of simplicity. Also instead of printing to standard output (stdout), messages are appended to a log. This log refers to a server log given the fact Agama flows run on a server-side engine.

How to check this log depends on the concrete engine used. For the case of Janssen, find more details [here](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/running/#logs-location).

**Note**: It is recommended to see this example in [action](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/running/index.md), however, logging in Agama is simple enough to follow just by reading the code.

## The `Log` directive

Logging a message is done via the [`Log`](https://docs.jans.io/stable/agama/language-reference/#logging) directive. The message to log is associated to a level, known as the "severity" level - a common concept in server side programming. For instance, in the case of Janssen levels are: *trace*, *debug*, *info*, *warn*, and *error*. Thus, every message conveys a "severity".

Quick facts:

- `Log` can be passed one or more parameters
- The message level may be specified in the first param by prefixing it with `@`. If not specified, a default severity is assumed, e.g. *info*
- The percent character `%` can be used as a placeholder for interpolation of literals or variables. The `%` character is effective as placeholder only if present in the first parameter
- Engines may print *map*s and *list*s partially, i.e. they may not be traversed wholly. In this case, the resulting message indicates part of the structure is missing

## Example

The above is clarified in the flow that follows:

```
Flow com.acme.basic.logging
    Basepath ""

Log "@warn My message"  // prints My message at warning level
Log "@w My message"      // A shortcut to the previous statement

Log "My message"         // prints My message at default level, e.g. info
Log "My" "message"       // The same. Params are printed separated by a space

car = { brand: "Simca", model: 1934, color: "yellow" }
Log car     // prints [(brand: Simca), (model: 1934), (color: yellow)]

notes = [ "do", "re", "mi" ]
Log notes   // prints [ do, re, mi ]
// beware: Log [ "do", "re", "mi" ] is syntactically invalid. It does not compile 

Log "Agama % %" "is" "concise"  // prints Agama is concise. Two placeholders replaced
Log "Agama % concise" "is"      // prints Agama is concise. One placeholder replaced

Log "Agama %" "is" "compact"      // prints Agama is compact. One placeholder replaced
Log "% %!" "Agama" "is compact"   // prints Agama is compact!. Two placeholders replaced

Log "My % % is %" car.model car.brand car.color   // prints My 1934 Simca is yellow

Log "%, %, %, %, end" 1 2 3    //prints 1, 2, 3, , end. One unmatched placeholder
Log "% of 10 is 3" "30%"       //prints 30% of 10 is 3. Percentage char is treated as such from second param onwards
Log "Interest rate is" "20%" "..."   // prints Interest rate is 20% ...

Finish true
```

Note this flow has no UI. It's not a requirement for an Agama flow to show a page. In this case, execution simply goes top to bottom with no intermediate pauses.
