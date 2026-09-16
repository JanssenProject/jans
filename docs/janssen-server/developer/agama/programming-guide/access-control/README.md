# Controlling flow access

In this page, a small project that mimics user authentication is studied. It helps to emphasize the importance of controlling access to critical flows for the sake of security.

The hypothetical login experience is as follows: the user is prompted to enter his username, if recognized, a random code is sent via SMS to his registered mobile phone. The user is asked to enter the code, if codes match, the user authenticates successfully.

**Note**: This is a toy example - not a recommendation on how user authentication should be in practice.

## Project dissection

The project consists of three flows:

- `com.acme.workaday.userValidation`: it prompts for a username. It searches the "database of known users" for a match and extracts the user's given name and phone. The flow returns these two values alongside the username in question.

- `com.acme.workaday.smsChallenge`. It receives a (displayable) name and a mobile phone number. This flow sends a random access code to the given number and prompts the user to enter such code. It finishes successfully if the value entered by the user is correct, otherwise it fails. 

- `com.acme.workaday.userauthn`: It is the main ("top-level" flow). It invokes `com.acme.workaday.userValidation` and then `com.acme.workaday.smsChallenge`.

### User validation

Flow [`com.acme.workaday.userValidation`](./project/code/com.acme.workaday.userValidation.flow) uses a hardcoded *map* of known users. They are stored in variable `people` as seen in the code. This was done so for the sake of simplicity and to keep the project as small as possible.

The user is given three attempts to enter a known username. Note the assignment in the `Repeat` loop:

```
iterations = Repeat 3 times max
    ...
```

This is Agama-valid: it helps developers count how many complete iterations were made once looping is done. If the loop is aborted earlier (by means of `Quit When`), such particular iteration does not count. 

User input is gathered by rendering template [`username.ftlh`](./project/web/username.ftlh). This resembles the template used in the [number guess game](../loops/README.md#a-number-guess-game). In the flow, the username is stored in variable `userId`, and the lookup in the `people` *map* is done this way:

```
userData = people.$userId
```

This [notation](https://docs.jans.io/stable/agama/language-reference/#maps-and-dot-notation) allows access the value associated to a key in a *map* where the key is only known at runtime, i.e., is variable. Hence, `userData` will be a *map* with keys `givenName` and `phone` for the user in question, or `null` if the lookup fails.

Note the loop is aborted when the lookup is successful. The conditional

```
When iterations is 3
    ...
```

is used to determine if three wrong attempts to lookup the username occurred. In such case, the flow just finishes with failure, otherwise a value like the below is returned to the caller:

```
{ success: true, data: { userId: "...", givenName: "...",  phone: "..." } }
```

where `userId` has the username in question. 

### SMS challenge

In [`com.acme.workaday.userauthn`](./project/code/com.acme.workaday.userauthn.flow), flow [`com.acme.workaday.smsChallenge`](./project/code/com.acme.workaday.smsChallenge.flow) is only triggered if the username validation was successful.

The challenge flow starts by generating a semi-random *string* containing six characters drawn from lowercase letters (a-z) and digits (0-9). Java developers will find the computation there odd but it is terse: just three lines. A proper computation would require onboarding external code however the project needs to be as compact as possible.

Then the next (commented) line follows:

```
//Call some.java.package#sendSMS conf phoneNumber name strRand
```

It conveys the idea of how a real SMS delivery functionality would be called. Instead, the random code is just being printed to the log. Testers keep an eye out for a log line that reads "Random code sent to".

The configuration required to send SMS is passed as parameter as well as the target mobile phone number. The name of the person and the random code would be used to format a good message.

A loop similar to that of the [number guess flow](../loops/project/code/com.acme.basic.numberguess.flow) is next. There, the flow is finished in case the user entered the right code. In case the maximum number of attempts is reached, the flow is finished passing the below:

```
obj = { success: false, error: "The number of allowed attempts has been exceeded" }
```

This is a common way to end flows that fail. The error message may be of use by the caller flow. 

### Main flow

There is no much to comment here besides the ways in which flow [`com.acme.workaday.userauthn`](./project/code/com.acme.workaday.userauthn.flow) can finish. If user validation failed, this flows finishes with failure too. If the user does not pass the SMS challenge, the flow finishes with

```
When obj.success is false
    Finish obj
```

so the error message previously seen is propagated to the caller - the browser in this case. When testing this particular route, the web browser will likely display "The number of allowed attempts has been exceeded" in the screen.

If the user passes the SMS challenge, the flow finishes with `userData.userId` which is syntactic sugar (a shortcut) for

```
{ success: true, data: { userId: userData.userId } }
```

This is a common way to end flows that succeed in the context of authentication flows. Here, a reference to the user that should be authenticated is [passed](https://docs.jans.io/stable/agama/language-reference/#flow-finish). If this flow is tested in Janssen Server, access would be granted as long as the `userId` is known by the server. For example, if `jsmith` is an existing, active user in Jans, then he would be successfully authenticated.

## Abuse and control

The login experience is started by launching the main flow `com.acme.workaday.userauthn`, however, nothing prevents a curious user from launching any of the other two flows directly. The consequences are serious. Let's see why.

Flow `com.acme.workaday.userValidation` finishes by passing a `userId` in case a valid user was entered. This means a user could be authenticated just by entering his username. That's really unsafe.

Regarding `com.acme.workaday.smsChallenge`, the situation is not better. This is an open door to arbitrarily send SMS messages. It's just a matter of passing any name and number in the input parameters. The service can be heavily abused by an attacker; this will not only cost money to Acme.

Attention need to be paid to the kind of functionalities flows expose. Sometimes this can be mitigated following a stricter flow design philosophy, however, this is not always doable, and there has to be a way to block certain flows to be launched directly.

[`project.json`](https://docs.jans.io/stable/agama/language-reference/#metadata) metadata descriptor allows developers  control these situations. Via `noDirectLaunch` property, it can be explicitly set what cannot be launched freely. Try editing this project's [descriptor](./project/project.json) with the following:

```json
{
  ...
  "noDirectLaunch": [ "com.acme.workaday.userValidation", "com.acme.workaday.smsChallenge" ]  
}
```

Generate the project archive again, and redeploy. It is safer now. Flows `com.acme.workaday.userValidation` and `com.acme.workaday.smsChallenge` will only be triggered by other flows, not the browser.
