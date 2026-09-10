# Flow configuration, inputs, and metadata

## On flow's header

Most flows seen so far have a very basic header: the `Flow` keyword and an indented `Basepath` directive. Three new directives will be covered here. They can enrich flows significantly and make a project more meaningful.

### Timeouts

Flows can be assigned a maximum amount of time for the end-user to fully complete them. This is done with `Timeout` where a number of seconds can be specified. This directive is expected to be right after `Basepath`, like this:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Timeout 100 seconds
```

Note the amount of seconds must be supplied as an unsigned integer literal value. Engines terminate flow execution when timeout occurs and take the user to an error page. Actually, engines should use an implict, default timeout value when a flow does not provide one. This avoids flows running indefinitely.

### Configuration

In practice, many times flows need some form of parameterization to propertly accomplish their goals. These parameters are configuration properties that remain the same regardless of when and how a flow is being used. Usually they are expected to be supplied by administrators upon project deployment.

Configuration properties supplied are bound to a variable (an Agama *map*) via `Configs`. This directive is expected to be after `Timeout`:

```
Flow com.acme.FoodSurvey
    Basepath "mydir"
    Timeout 100 seconds
    Configs conf
```

Here `conf` will contain whatever the administrator supplied as configuration parameters for this flow and it is accessible as a regular variable in the flow code. How to set configuration is an engine-dependant issue. ["Running the examples"](../running.md#project-deployment), covers how to do so in the case of Janssen server. 

As an example, assume `com.acme.FoodSurvey` at some point has to send an e-mail. For this, the outgoing mail server details need to be supplied. For instance, if they look like:

```yaml
host: smtp.acme.com
port: 587
connectProtection: StartTLS
fromName: Acme
fromEmailAddress: noreply@acme.org
smtpAuthentication:
  username: admin@acme.org
  password: secret
```

then `conf` would be structured this way:

```
{ host: "smtp.acme.com", port: 587, connectProtection: "StartTLS",
  fromName: "Acme", fromEmailAddress: "noreply@acme.org",
  smtpAuthentication: { username: "admin@acme.org", password: "secret" } }
```

### Inputs

Inputs are parameters used to drive flow behavior - think of the arguments passed when calling a function or method in a programming language. They are directly supplied by callers at runtime.

So far none of the flow examples has input parameters, however, most of times flows will need to receive one or more input parameters in practice. For instance, recall the [number guess game](../loops/project/code/com.acme.basic.numberguess.flow) where the user is requested to enter numbers between 0 and 5, and expected to guess a secret random number in 3 attempts at most. This is fairly static behavior. A good improvement would be to parameterize the interval used for guessing and the maximum number of attempts. Here is where [`Inputs`](https://docs.jans.io/stable/agama/language-reference/#inputs) comes to the spotlight:

```
Flow com.acme.basic.numberguess
    Basepath "guess"
    Inputs interval maxAttempts
```

`Inputs` is supplied after `Configs`, however, with `Configs` and `Timeout` being optional, the above is valid. After the directive name, several variable names can be supplied. When the flow runs, such variables (`interval` and `maxAttempts` in this case) will contain the values passed by the caller of this flow.

#### What is exactly the "caller"? 

So far, if the reader has been testing the examples, has noticed flows are basically invoked directly from the browser with the help of some web-like application for building a lengthy "launch URL". In the next chapter it will be seen how a flow can invoke another flow so essentially there are two type of callers.

The way callers pass inputs to flows differ: browsers send inputs in the [launch URL](../running.md#sending-parameters) while flows do it directly in Agama code using the `Trigger` directive. This directive will be studied in an upcoming lesson.

## Project metadata

Projects presented in this series are structured a minimalist way. However, note besides flow files and templates, real-world projects may contain foreign language code and libraries, descriptors, localized UI labels, and web-related assets like javascript, stylesheets, images, etc. This is described more formally in ["The .gama file format"](https://docs.jans.io/stable/agama/gama-format/).

Of big importance is [`project.json`](https://docs.jans.io/stable/agama/gama-format/) - a descriptor file found at the project's root that contains metadata in JSON format about the project contents. In the current context, the `configs` section is of special interest.

`configs` is expected to be a JSON object resembling how the configuration properties of flows in the project may look like. These are not the actual configuration properties - just sample data. Agama projects can be distributed freely so having confidential bits like addresses, passwords, secrets, etc., easily discovered by others is not acceptable.

`configs` consists of several JSON objects. One for every flow requiring configuration properties. The example below shows the content of a `project.json` file for a project consisting of two authentication flows. One which prompts the user to enter his phone number where a passcode will be sent via SMS (`com.acme.authn.sms`), and another for a typical username/password authentication (`com.acme.authn.userpwd`).

```json
{
  "projectName": "acme-auth",
  "version": "1.0.0",
  "author": "Wile E. Coyote",
  "license": "apache-2.0",
  "description": "Allows users to authenticate to Acme portal",
  "configs": {
    "com.acme.authn.sms": { 
      "accountId": "your account ID",
      "fromNumber": "+1 123 456 7890",
      "authToken": "blah...blah.."
    },
    "com.acme.authn.userpwd" : {
      "blockAccount": {
         "afterAttempts": 4,
         "timeFrameSeconds": 30
      }
    }
  }
}
```

Flow `com.acme.authn.sms` uses an online SMS delivery service. The service provider assigns Acme an account ID, an authentication token, and a number from which messages are sent. With these three pieces of data, the flow can send  messages by calling the provider's SMS API. However note the details in `project.json` are not what the provider shared with Acme, just sample/dummy data.   

Flow `com.acme.authn.userpwd` blocks a user account if a certain number of failed attempts to enter the correct password occur within a given time frame.

## What's next?

This concludes the fundamentals of Agama. From here onwards, examples will bundle project metadata and will make use of  features like inputs and timeouts. More elaborated flows come.
