# Onboarding custom code

A project that bundles foreign code will be studied here. This project requires basic knowledge of the Java language and potentially a Janssen server installation (for testing purposes). Recall Agama engines may support different languages and differ in how to incorporate such foreign code or their associated binaries.

In the particular case of Janssen, the engine makes use of a [Groovy scripting environment](https://groovy-lang.org/integrating.html#_groovyscriptengine) allowing usage of Java or Groovy source code which is executed at runtime, avoiding the need of supplying any intermediate or compiled code, like byte-code, in an Agama project.

The root of the (Java) package hierarchy is located in the `lib` folder of a project; `lib` is part of the [gama format](https://docs.jans.io/stable/agama/gama-format/). For the current example, it is the directory located [here](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/lib) and includes a couple of Java classes belonging to package `com.acme.agama.survey`.

## The club strikes again

Probably readers acknowledge how the "unforgiving club" has been helpful in their journey of learning Agama. The club is back again and now requires their members to take a survey. This will help the board of prominent members take better decisions, specially for budgeting...

Requirements say the board should be able to supply the questions and possible answers in a straightforward manner, and programming knowledge should not be a requisite. A grasp of HTML (forms) is fine. Also, adding, removing, or re-ordering questions has to be done easily. Let's see how Agama saves the day again.

## End-user flow at a glance

Initially, users will be presented an optional welcome page where they will be kindly asked to spend some minutes to answer a few questions. Then the survey starts showing the questions in the order as set by the board (via project configuration), with the possibility to navigate backwards and forwards to revise or update as needed.

In the final question, an "end" button will be shown that ultimately takes to a "thank you" page.

## Project analysis

There is a single flow: [`com.acme.workaday.club_survey`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/code/com.acme.workaday.club_survey.flow).

### Configuration

Note the use of `Configs` directive. Here is an example of how Agama variable `conf` may look like (see [`project.json`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/project.json)):

```
{
  showIntro: true,
  pages: [
      { id: "recommend", template: "recommend.ftlh" },
      { id: "entertain", template: "meetings_entertaining.ftlh" },
      { id: "topics",    template: "meetings_newtopics.ftlh" },
      { id: "frequency", template: "meetings_frequency.ftlh" },
      { id: "payment",   template: "paid_membership.ftlh" }
  ]
}
```

`showIntro` determines if the "welcome page" should be displayed, while `pages` is the list of (question) HTML pages to display. Every page is associated an identifier and a physical location. The templates for this example can be found [here](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/q). Every page features a single answer.

Note these templates are made up of common markup, with the only exception of the wrapping `macro` tag. This is a freemaker directive that will be regarded soon.

### Welcome page

The flow file starts with self-explanatory code that will render the page [`intro.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/intro.ftlh) if necessary.

So far the UI in all of the projects has been fairly plain. This has helped to set distractions aside because real-world HTML/CSS may become dense and cryptic. Nonetheless, this project brings some CSS and Javascript to the table without adding a significant overload. Note this welcome page "links" [Bootstrap](https://getbootstrap.com/) through a CDN, and contains a heading, some text, and a submission button.

No user feedback is grabbed from this page.

### Answers Handler

Class [`com.acme.agama.survey.AnswersHandler`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/lib/com/acme/agama/survey/AnswersHandler.java) helps maintain the "state" of the survey as the flow runs by saving the selections the user has made so far. This class holds a Java list of [`com.acme.agama.survey.Answer`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/lib/com/acme/agama/survey/Answers.java) objects. An `Answer` consists of an identifier (a template identifier), a list of choices, and some text. The choices are identifiers as well: they map to the values of the input fields of the given template. When there is a single-choice question, the list will be of size one.

A question may also require the user to enter explanations or further details about an answer. That's why `Answer` class has a `text` member. A `null` value is stored when the question does not make use of this feature.

Important remarks about methods in [`AnswersHandler`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/lib/com/acme/agama/survey/AnswersHandler.java):

- `getAnswer` retrieves an `Answer` object from the list of `Answer`s based on the `id` supplied (`null` if the lookup fails)
- `storeAnswer` adds a new `Answer` to the list or updates the `Answer` if already present. When the user has made no selections (`choiceIds` is null), an empty list is stored
- A zero-parameters constructor is there for convenience: every time an `RRF` directive is hit, the Jans engine will try to serialize the flow variables. Such constructor allows `AnswersHandler` objects to be serializable. The same applies to `Answer`

### The loop

Here is where the main flow logic resides. At every iteration, a question is shown. The template to use from the list of pages supplied in the configuration is pointed by variable `index`. The first question is associated with `index` zero.

Before rendering a question, `ah` (the instance of `AnswersHandler`) is used to recover the `Answer` associated to the current question in case it already exists. This is stored in variable `ans`. Note an `Answer` may already exist because the flow allows to move to previous questions and modify the choices made earlier.

Rendering is carried out using template [`survey.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/survey.ftlh) which is injected with the following:

- The `Answer` (stored in `ans`)
- The template location for the current question
- Whether the current question is the first
- Whether the current question is the last one

The last two pieces are boolean values computed via method `isFirstLast` on `ah`. It returns a Java [`Map.Entry`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.Entry.html) which is stored in variable `fl`. This is an easy way to return a tuple given that Java does not support this kind of structures natively. Note values in `fl` are accessed as if `fl` were map, however, this is actually invoking methods `getKey` and `getValue`. This approach was already used in one of the [foreign code](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/foreign/project/code/com.acme.basic.foreign_2.flow) examples.

### The form

Template `survey.ftlh` has an HTML form with buttons to go forward, backwards, or terminate. They are shown depending on the values in the `fl` tuple.

At the top of the file, there is a template [import](https://freemarker.apache.org/docs/ref_directive_import.html). This is key to include the markup for the current question. The template to be included is derived from the expression `"q/" + template` which evaluates to the physical path of the template. This is assigned to namespace `ns` - this is an arbitrary name.

Inside the form, the [macro](https://freemarker.apache.org/docs/dgui_misc_userdefdir) named `main` is invoked with `<@ns.main />`. This effectively "injects" the required markup. Note all the example templates in directory [q](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/q) have a macro with such name. An alternative approach could have been using [include](https://freemarker.apache.org/docs/ref_directive_include.html) instead, however its usage is discouraged. `import` is more powerful and is better fitted for future improvements, like usage of parameters in question templates.

Another important convention followed by question templates in this project is the naming of form fields. It is assumed all input fields are named `option` in the HTML markup. This suits well for a set of checkboxes or radio buttons. For free editable text, the name `text` must be used.

Also, there is some Javascript code employed to fill the form controls with decisions and text that may have been previously entered. Recall the flow allows to navigate backwards and forwards so earlier selections have to be "restored". The analysis of the Javascript logic is left as an exercise to the reader.

Once `RRF` returns, method `storeAnswer` is called by passing both `option` and `text` values, as well as the question identifier.

### Navigation

Then, the decision of which question to show next comes. This is achieved by inspecting how the form was submitted. Every submission button in [`survey.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/survey.ftlh) has a different name. Upon submission only one of those names will be present in the received data (variable `obj`). This way, it can be decided if the user is done (button `end`), if he is moving backwards (`prev`) or forwards (`next`).

Moving forwards or backwards requires decreasing or increasing variable `index`. Since Agama cannot do any arithmetic directly, class [`java.lang.Math`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html) is employed.

### Wrapping up

The user has enough (one hundred) navigation attempts according to `Repeat` to finish the survey. Once he is done, method `close` is invoked on `ah`. This is an empty method now, but in practice, developers have to do something with the gathered data instead of ignoring it silently! - club members won't be happy with that. By now, answers are printed to the server log with `Log "Answers were" ah`. This results in the invocation of `toString` on `ah` which in turn calls `toString` on every `Answer`.

Before terminating, the page [`outro.ftlh`](https://docs.jans.io/head/janssen-server/developer/agama/programming-guide/custom-code/project/web/outro.ftlh) is shown. It displays a static "thank you" message and a submission button.
