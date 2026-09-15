# Loops

Agama provides two mechanisms to write loops: [`Repeat`](https://docs.jans.io/stable/agama/language-reference/#repeat) and [`Iterate Over`](https://docs.jans.io/stable/agama/language-reference#iterate-over). This page will cover `Repeat` only. Readers are encouraged to write some code using `Iterate Over`.

## A number guess game
    
In this example a simple "number guess" game is tackled. In flow [`com.acme.basic.numberguess`](./project/code/com.acme.basic.numberguess.flow), a random integer between zero and five is generated, and then the user is given up to three chances to guess the number. A dissection of the code follows.

These lines generate the random number and convert it to string:

```
sr = Call java.security.SecureRandom#new
rand = Call sr nextInt 6
strRand = Call rand toString
```

In an earlier [example](../foreign/README.md#pseudo-random-number-generation) the same approach to generate a random number was used. Conversion to string is needed because data retrieved from web forms are received as strings. In Agama, comparing values of different data types (like a number and a string)  with `is` operator, always evaluates `false`.

Then the loop part comes:

```
data = { failed: false }
Repeat 3 times max
    obj = RRF "number_guess.ftlh" data
```

`data` contains info to be passed to the guessing page. `Repeat` is followed by how many times the indented block of code will be executed. Note loops can terminate earlier - this will  be seen in the second example.

Next the [guessing](./project/web/guess/number_guess.ftlh) page is presented to the user. It looks like this:

```html
<!doctype html>
<html xmlns="http://www.w3.org/1999/xhtml">
<body>
    <h1>Guess a number between 0 and 5</h1>
    <p style="color:red">
        <#if failed>
            Wrong number!
        </#if>
    </p>
    <form method="post" enctype="application/x-www-form-urlencoded">
        <input type="number" name="guess" step="1" max="5" min="0" required autofocus>
        <input type="submit" value="I feel lucky!">
    </form>
</body>
</html>
```

As expected, there is a form where a number can be entered. Additionally, at the top a message is presented based on the (boolean) value of `failed`. This will be `true` if the user enters a wrong number in a previous form submission.

A conditional follows:

```
When obj.guess is strRand
    RRF "congrats.ftlh"
    Finish true
```

`obj.guess` has the value the user entered and is compared to match with `strRand`. When they are equal a [congratulations](./project/web/guess/congrats.ftlh) page is shown, and then the flow finish successfully. If that is not the case, the below is executed before the loop starts again:

```
data.failed = true
Log "Attempt % failed" idx[0]
```

A value is set for `failed` so the "Wrong number!" message is shown in the next `RRF` execution. Finally a message is logged. Here `idx[0]` contains the current index of the iteration, so the first failed attempt adds to the log "attempt 0 failed". If there were a nested loop inside `Repeat`, the index of the inner iteration would be accessed with `idx[1]` and so on. `idx` is an implicit variable in Agama.

Once three failed attempts occur, the next line executed is `Finish false` terminating the flow. 

**A note on Basepath**: Readers may have noticed this flow has `Basepath "guess"` in its header. This indicates that paths to templates passed in RRF are relative to the `guess` directory found inside `web` folder of the project. `Basepath` helps achieve a better organization of UI assets.   

## Club referrals

[Earlier](../conditionals-matching/README.md#example-the-unforgiving-club), an example regarded the procedure for admission to a peculiar club. Once accepted, the new member fills a form for referring friends so they can get invitations to join too.

The rules are simple: at minimum one and at maximum three referrals can be supplied. This behavior is implemented in flow [`com.acme.basic.referrals`](./project/code/com.acme.basic.referrals.flow). A dissection of the code follows.

Before entering the loop, a couple of initialization statements:

```
refs = [ ]
pageData = { first: true }
```

At the end of the flow, the list `refs` will contain the data (name and e-mail) of all referrals. `pageData` is used to pass two things to the template: whether this is the first loop iteration and the name of the last contact submitted.

The first part of the loop is:

```
Repeat 3 times max
    data = RRF "referred.ftlh" pageData
    name = data.name
    
    Quit When name is null
```

The [page](./project/web/referrals/referred.ftlh) to enter a referral will be displayed at most three times. It has two sections:

- The top section is shown only after a first referral has been added. It displays a link that submits an empty form that the flow will interpret as no more contacts will be submitted
- The lower section is always shown and has a form with fields for entering name and e-mail

The `name` variable in the flow will contain whatever was filled in the `name` form field. The loop is interrupted in case the value of `name` is `null`, in other words, if the link at the top of the page was clicked. This is achieved via `Quit` which is followed by a `When` clause. If the conditional evaluates `true` the loop is aborted, otherwise the remaining statements are executed and the loop continues.

So if referral data was submitted, the following is executed:

```
it = idx[0]    
refs[it] = data
Log "% - added %" it data.name

pageData = { first: false, name: name }
```

This "appends" the data entered (name and e-mail) to the list of referrals (`refs`). Given that `it` contains the iteration index, `refs[it]` refers to a yet unassigned position in refs. This makes the list grow at every iteration. Then a message is added to the log.

Finally, `pageData` is updated with the last `name` entered, and the key `first` is assigned `false` before going to the next iteration. 

Once the loop has ended, a final page is displayed and the flow finishes: 

```
pageData = { referrals: refs } 
RRF "thanks.ftlh" pageData
Finish true
```

`pageData` is overwritten with a map containing the elicited data and then passed to ["thank you"](./project/web/referrals/thanks.ftlh) template in `RRF`. This page displays a summary of the contacts supplied: a comma-separated list of their names.

## On flow interruption

`Quit When` is expected to be found at the top level of a loop at most once. In other words, the below are all illegal:

```
Repeat some times max
    ...
    Quit When it is raining
    ...
    Quit When day is cloudy
    ...
```


```
Repeat some times max
    ...
    When there is hope
        ...
        Quit When day is cloudy
        ...
    ...
```

This forces developers write flows whose "interruption logic" is easier to understand and contributes to simplicity because there can be only one "exit point". Hence, the below is legal:

```
Repeat some times max
    ...
    Repeat some_more times max    
        ...
        Quit When it is raining
        ...
    ...
    Quit When day is cloudy
    ...
```

<!--This is expected to be a boolean value, however, if `failed` is not present in the map passed to the template, it defaults to `false` - that's what the expression `failed!false` means. See Freemarker's [Default value operator](https://freemarker.apache.org/docs/dgui_template_exp.html#dgui_template_exp_missing_default).
 however it is an empty map initially -->