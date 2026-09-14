# Agama basics and Hello world

## Projects structure

Before addressing the examples, a preliminary understanding of how a project is structured is required. At the top of a project directory, several folders can be found. The following are the minimal required:

- `code`: It holds all flows part of a project. Every flow - implemented in Agama language - resides in a separate file with `.flow` extension

- `web`: It holds web assets like UI templates, stylesheets, images, localization strings, etc.

In-depth details about the anatomy of a project can be found in the official documentation page: ["The .gama file format"](https://docs.jans.io/stable/agama/gama-format/).

Every flow must be identified by a "qualified" name: a sufficiently expressive name to give an idea of what the flow is about. By convention, qualified names adhere to a reverse Internet domain name notation. As an example, if a flow asks the user about their food preferences, a qualified name could be `com.acme.FoodSurvey`. Thus, the corresponding physical file should be `com.acme.FoodSurvey.flow`.

## Language traits

Agama is an imperative language with a terse, non-distracting syntax. An Agama flow (`.flow` file) is expected to depict the structure of a web flow only, while low-level details and computations have to be delegated to an underlying (foreign) language. For instance, the Janssen engine provides an easy and convenient way to call Java/Groovy code from Agama flows.

The language consists of a series of directives that can be passed parameters. Parameters are supplied after the name of the directive and are separated by spaces: there is no need to wrap parameters in parenthesis and use commas to separate them. Actually the language does not support parenthesis.

Also, there are no curly braces (`{ }`) - nesting of code blocks is made using Python-like indentation.

## Hello world

Here, a salutation message will be displayed on the user's browser. It is very straightforward.

### Basic flow directives

An Agama flow consists of a header and one or more statements following. A minimal header for this example would look like:

```
Flow com.acme.basic.helloworld
    Basepath ""
```

Flows start with the `Flow` keyword followed by the qualified name of the flow. For more information on this topic check the [Header basics](https://docs.jans.io/stable/agama/language-reference/#header-basics) section of the language reference.

The statements following the header contain the actual flow implementation. In this case, a web page with a salutation will be shown. This is how it is done: 

```
RRF "salutation.ftlh"
```

RRF is a powerful language directive. However in this case it does a very simple thing: it replies the content of file `salutation.ftlh` to the user's browser. It's kind of "serving" the file. Here is how it looks like:

```html
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
    <h1>Hello world!</h1>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="submit" value="Continue">
    </form>
</body>
</html>
```

The reason for the file extension (ftlh) is related to the FreeMarker Template Language - the templating technology used by the Janssen engine. The *h* in *ftlh* stands for HTML. This topic will be regarded in the upcoming example.

**Note**: It is assumed this file resides inside `web` folder of the project.

In practice, web flows almost always gather some data from the user: passwords, codes, gestures, etc. `RRF` not only serves content but "waits" (without blocking) until some data is received. The way data is sent back to the server is via HTML POST form submission. This is the reason `salutation.ftlh` has a form with a submit button.

Once data is submitted - in this case an empty POST - `RRF` resumes and execution continues for the next statement found in the flow file.

To terminate a flow the directive `Finish` is used. It is analog to the `exit` function of the `C` language. A flow must signal whether termination was successful or not, and may optionally attach some extra data. This is very useful specially in authentication scenarios. 

For this example, we can merely use:

```
Finish true
```

This indicates success. 

**Note**: If this project is tested in a Janssen Server, after submitting the form, the browser will be taken to an error page. This is because in Janssen, Agama flows run as authentication flows where the identity of the user to authenticate must be passed in the `Finish` directive. In later examples, this topic is explored.

### Flow code

So here's how the contents of file `com.acme.basic.helloworld.flow` look like:

```
Flow com.acme.basic.helloworld
    Basepath ""
    
RRF "salutation.ftlh"
Finish true
```

Note the statements of the flow "body" are aligned with the opening `Flow` keyword.

### Project structure

Here's how this project is laid out in the filesystem:

```
├── code
|    \─── com.acme.basic.helloworld.flow
\── web
     \─── salutation.ftlh
```

This effectively reflects the contents of the [project](./project) directory.
