# Collecting user input

The example covered in this page takes the ["Hello world"](../basics-hello-world/README.md) example a bit further. This project will have a flow that shows a custom salutation instead of a static "hello" message. For this, a form will be initially presented to the user asking to enter his nickname, then after submission, a second page will be shown.

## About types and variables

Agama supports data types like _string_, _boolean_, _number_, _list_ or _map_. The [language reference](https://docs.jans.io/stable/agama/language-reference/) page explores types and variables manipulation in detail. 

Some relevant facts before proceeding with the example:

- There is no strict type enforcement in Agama like in most dynamic scripting languages
- Variables are not declared, just assigned and used freely
- Variables can be re-assigned several times in the same flow
- Variables are always global in a given flow

## Project layout

On disk, the project will look like the below:

```
├── code
|    \─── com.acme.basic.salutation.flow
\── web
     \─── prompt.ftlh
     \─── salutation.ftlh
```

## Flow code

The salutation flow is similar to `com.acme.basic.helloworld` but two pages will be shown instead of one:

```
Flow com.acme.basic.salutation
    Basepath ""

data = RRF "prompt.ftlh"
RRF "salutation.ftlh" data
Finish true
```

The first statement in the body, i.e. `data = RRF "prompt.ftlh"` does the following:

- Shows a (static) page with an input field where the user is expected to enter his nickname
- After submission, the field values in the form are bound to variable `data` (an Agama _map_). It is basically an associative array or dictionary

Contents of `prompt.ftlh` are:

```
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
    <h1>What's your nickname?</h1>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="text" name="nickname" required>
        <input type="submit" value="Continue">
    </form>
</body>
</html>
```

Once the HTTP POST occurs, variable `data` will have a value like: `{ nickname: "Baggles" }` (assuming "Baggles" was entered in the text field).

**Important facts:**

- `RRF` returns a dictionary whose keys are the names of the form fields and the associated values correspond to the actual data sent. Values are always treated as Agama *string*s
- If the submitted form has no fields, an empty map is returned: `{ }`
- The returned _map_ can be optionally assigned to a variable

### RRF and dynamic content serving

Before sending content to the browser, `RRF` performs a process of rendering. This implies reading the contents of the (HTML) template whose path is passed as the first parameter, and inject data passed in the second parameter, if any. The resulting markup is what is actually replied. This process allows serving dynamic content instead of plain static markup.

There are different technologies for rendering UI templates. For instance, Janssen uses [Apache Freemarker](https://freemarker.apache.org). This is better understood with the example itself.

For the second `RRF` directive in flow `com.acme.basic.salutation`, two parameters are being passed: `"salutation.ftlh"` and `data`. Here `salutation.ftlh` is a Freemarker template located under the `web` directory of the project. Its contents look like:

```
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
    <h1>Hello, ${nickname}!</h1>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="submit" value="Finish">
    </form>
</body>
</html>
```

Here, the `${nickname}` expression inserts the value associated to the key `nickname` in the map passed as second parameter to `RRF`, namely `data`. To the average developer, Freemarker templates are very easy to read (and write) and in cases like this, they look a lot like pure HTML pages. Also it provides auto-escaping for free avoiding malicious markup injection.

Once the form is submitted, execution continues at the next line (`Finish true`) and the flow terminates successfully.
