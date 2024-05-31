# Onboarding custom authentication methods

Out-of-the-box Casa supports some useful authentication methods for a secure, pleasant authentication experience. Adding more authentication mechanisms is possible and requires certain development effort. In this guide we summarize the steps required to do so and give you some useful pointers to start your coding journey.

Supporting a new authentication mechanisms consists of two tasks: coding a custom interception script and creating a plugin that contributes an authentication method. The former has to do with the authentication flow the user experiences (to access Casa or other apps), and the latter with the credential enrollment process.

## Interception script

!!! Note
    Acquaintance with [person authentication](../../admin/developer/scripts/person-authentication.md) scripts is required

### About Casa authentication flow

Casa's authentication is backed by a main custom script that determines which authentication methods are currently supported, dynamically imports relevant custom scripts, and orchestrates the general flow while delegating specific implementation details to some of those scripts.

The main script supports backtracking: if a user is asked to present a specific credential and that credential isn't currently available, he can choose an alternative credential by visiting a different page corresponding to the alternative authentication method. Users can backtrack any number of times.

### Script requisites

To code the script corresponding to the authentication method to add, use the `.py` script found [here](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-casa/plugins/samples/sample-cred) as a canvas. Ensure the following conditions are met so that it properly integrates in the main Casa flow:

- For step 1, `prepareForStep` must only return `True`  
- For step 1, `getExtraParametersForStep` must only return `None`  
- For step 1, the `authenticate` routine must check if there is already an authenticated user, and if so bypass validating the username and password. This is because a user may have previously attempted authentication with a different method
- `hasEnrollments` routine has a signature like:  
       `def hasEnrollments(self, configurationAttributes, user):`  
  where the `configurationAttributes` parameter is a `java.util.Map<String, io.jans.model.SimpleCustomProperty>` and `user` is an instance belonging to `io.jans.as.common.model.common.User`
- `hasEnrollments` must return `True` or `False`, describing whether `user` has one or more credentials enrolled for the type you are interested in  
- Keep in mind that `getPageForStep` won't be called when `step=1` in your script. Casa takes charge of this specific step/method combination  
- Finally, ensure that custom pages returned by `getPageForStep` for step 2 (or higher) contain the fragment:

    ```
    <ui:include src="/casa/casa.xhtml" />
    ```

    This will display a set of links for the user to navigate to alternate 2FA pages. The list will be shown when clicking on a link which should be provided this way:
    
    ```
    <a href="javascript:showAlternative('ELEMENT_ID')" id="alter_link" class="green hover-green f7-cust">#{msgs['casa.alternative']}</a>
    ```
    
    Here `ELEMENT_ID` is the identifier for the HTML node that wraps all visual elements of your page (excluding `casa.xhtml`). It is required to preserve `alter_link` as `id` for the `a` tag.

Adding the script to the server can be done via [TUI](../../admin/config-guide/config-tools/jans-tui/README.md) for instance. Ensure the display name assigned to the script is short and meaningful. Check [here](../administration/quick-start.md#enable-scripts) for examples. This value will be used as "ACR" in the plugin that will have to be developed for the credential enrollment process.

### Key questions

As you code the script, you will come up with some design decisions, for instance: 

- How to model and store credentials associated to the authentication method? 
- What kind of parameters are relevant for the authentication method?
- What's the algorithm for authenticating users once they have supplied a valid username/password combination?

Depending on the answers, you may like to start instead with plugin development first. This is not always the case though, however, getting your hands on the plugin might help unclutter the path. 

## Enrollment plugin

Coding a Casa plugin is mainly a Java development task. You can use the "Sample credential" [plugin](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-casa/plugins/samples/sample-cred) as a template to start the work. Ensure you have a development environment with:

- Java 11 or higher
- Maven 3.8
- A SSH client tool
- Access to a Jans Server installation that includes Jans Casa. Prefer a VM installation over the CN edition for development purposes

### Plugin deployment

Start with deploying the plugin to get acquainted with the process:

1. Download the `sample-cred` project folder to the local development machine and `cd` to it. You can download the jans repository [here](https://github.com/JanssenProject/jans/archive/refs/heads/vreplace-janssen-version.zip)
1. Run `mvn -o -Dmaven.test.skip package`
1. This will generate a `target` folder with a couple of jar files in it

Access Casa admin console and in the plugins page, upload the file suffixed with `jar-with-dependencies.jar`. After one minute approximately, in a browser hit `https://<hostname>/jans-casa/pl/sample-cred-plugin/user/cred_details.zul`. You will get access to a dummy page.

You can remove the plugin and add it as many times as you like - no restarts are needed. You can do so either via the admin console or by dropping/removing the file directly in the filesystem (the path is `/opt/jans/jetty/jans-casa/plugins`)

### Study the sample project

Now it's time for you to go through the project folder checking one file at a time. Most of files contain comments that explain the purpose of things.

Once you are done, analyze the file `./src/main/resources/assets/user/cred_details.zul`. It contains the markup of the page you visited earlier.

### Enable the authentication method

Once the [interception script](#interception-script) is added to the server (a draft is OK), visit the admin console and enable the [authentication method](../administration/quick-start.md#enable-methods-in-casa) by assigning the plugin just loaded. From here onwards, the left-hand side menu of Casa will have a new item under the "2FA credentials" heading. Additionally a new panel in the user's dashboard will appear and show some detail about the authentication method. 

### Additional tweaks

If you alter the `.zul` file and then package and redeploy the plugin, you will most probably not see any change taking effect in the UI page. This is because the ZK framework caches the `.zul` pages by default for a very long period. To change this behavior do the following:

1. Connect to your VM and `cd` to `/opt/gluu/jetty/jans-casa/webapps`
1. Extract ZK descriptor: 
    ```
    # jar -xf jans-casa.war WEB-INF/zk.xml
    ```    
1. Locate XML tag `file-check-period` and remove it including its surrounding parent `desktop-config`
1. Save the file and patch the application war:
    ```
    # jar -uf jans-casa.war WEB-INF/zk.xml
    ```
1. Restart casa (e.g. `systemctl casa restart`)

From now on, any template change will take effect after 5 seconds.
