---
tags:
  - administration
  - developer
  - agama
  - agama lab
  - quick start
---

# Quick start using Agama Lab

In this quick start guide, we will build, deploy and test a simple Agama 
project using the [Agama Lab](https://cloud.gluu.org/agama-lab). 
This project will enable user authentication using password when deployed on
an IDP.

Major Steps involved in this process are:

- [Create a new Agama project on the Agama Lab](#create-a-new-agama-project)
- [Defining the authentication flow](#defining-the-authentication-flow)
- [Design the user interface](#design-user-interface)
- [Deploy Agama project](#deploy-agama-project)
- [Test using Tarp](#testing-using-janssen-tarp)

This guide covers steps above in the following sections.

## Prerequisites

- We will use the [Janssen Server](https://jans.io) to deploy the Agama project.
Follow the instructions [here](https://docs.jans.io/head/janssen-server/install/)
to install the Janssen Server.
- You will need a GitHub account where Agama Lab GitHub app has been [integrated](https://gluu.org/agama/sign-up-sign-in/). GitHub
account can be a personal account or an organizational account. For this guide
we will assume that a personal account is being used.
- Two new public GitHub repositories in your GitHub Account. These repositories
will be used as [development repository](https://gluu.org/agama/flow-designer/) and [release repository](https://gluu.org/agama/flow-designer/). Each repository should have at least one 
commit. 
- Agama Lab should have permissions to access the development and the release repositories. This can be configured
using the [Agama Lab GitHub app](https://gluu.org/agama/how-to-integrate-agama%e2%80%90lab-github-app-with-your-github-account/) in your user account. 


## Create a new Agama project

We will use [Agama Lab](https://cloud.gluu.org/agama-lab/) to create a simple 
username-password based user authentication project. The authentication flow 
will be similar to the one shown below.

![](../../../assets/image-agama-lab-flow-quick-start-guide.png)

An Agama project may include multiple components. For example, an authentication
flow, a user interface, assets like organization's logo, external libraries, etc. 
In this project, we will see how to  

- Define the authentication steps of a flow
- Design the user interface to capture user inputs

Let's get started.

### Log into the Agama Lab


1. Go to [Agama Lab](https://cloud.gluu.org/agama-lab) login page. Sign in using
GitHub.

    ![](../../../assets/agama-loginpage.png)

2. Authenticate on the GitHub authentication page.

    ![](../../../assets/agama-lab-git-login.png)

3. After successful sign in, the [project dashboard](https://gluu.org/agama/agama-lab-project-dashboard/)
will be shown. Next step is to select the repository where you want to create 
the project. Select the `development` repository by clicking on 
`Change Repository` -> `Your Repositories` -> select the repository -> `Select`.
   
    ![](../../../assets/agama-lab-add-git-repo.png)


4. The project dashboard with all the existing projects (if any) from the GitHub
repository will be shown. New projects can be created, edited, and deleted 
from this project listing page.

    ![](../../../assets/agamalab-project-page.png)

### Create a new project

1. To create a new project, click on the `New Project` button.
   Enter the name and the description for the new project and click the `Create` button.

    ![](../../../assets/agamalab-new-proj.png)

2. This will create a new project.

    ![](../../../assets/agamalab-project-listing.png)

### Defining the authentication flow

1. Create a flow file

    Open the newly created project by clicking on :material-arrow-top-right:.
    This will open the project in the [project editor](https://gluu.org/agama/project-editor/). 
    
    To start creating the flow, right-click on the `code` directory > `New` > `Flow File`.

    ![](../../../assets/agamalab-new-flow.png)

    Give name and description for the flow file and then click `Create`.

    ![](../../../assets/agamalab-new-flow-name.png)

    A newly created flow file contains one `start` block.

    ![](../../../assets/agamalab-flow-passwd-1.png)
    

    In the subsequent steps, we are going to create nodes in the flow using the
    flow editor. Refer to [this](https://gluu.org/agama/project-editor/) guide if you need to understand how 
    to create, delete and configure nodes.

2. Create `Get class reference` Call block

    To perform authentication we need to validate username and password 
    provided by the user with the IDP. This operation is done using an instance
    reference of `AuthenticationService` class.

    To get the instance reference from the IDP, we first need the class
    reference of the `AuthenticationService` class. That is what we will do 
    in this step.

    Drag the colored dot available on the right edge of the `start` node to 
    create the next node in the flow. A list of available node types will be
    presented. 

    ![](../../../assets/agamalab-flow-passwd-create-call.png)

    Select `Call` node. A new `Call` block should appear with a link to the 
    `Start` block

    ![](../../../assets/agamalab-flow-passwd-new-call.png)

    Click the `Call` block and then click :material-pencil: to open the 
    configuration screen. Add configuration values as shown below.  
    This configuration will fetch a class reference of the 
    `AuthenticationService` class and 
    store the reference in a variable called `authServiceClass`. This reference will 
    be used to fetch an instance of this class from IDP in the subsequent steps.

    `Call type`: `Call static method`

    `class name`: `io.jans.as.server.service.AuthenticationService`

    `method name`: `class`

    `assign result to`: `authServiceClass`

    ![](../../../assets/image-agama-lab-edit-authn-instance.png)

3.  Create `Get instance reference` Call block

    In this step, we will fetch the instance reference of the `AuthenticationService`
    using the class reference that we obtained in the previous step. To fetch this 
    instance reference from the IDP, we will use `CdiUtil` class's `bean` method and
    pass the class reference of the `AuthenticationService`  
    as an input to it. Use the steps and configuration below to do this.

    After the `Get class reference` node, create a new `Call` node.
   
    Click on the newly created `Call` block and by clicking :material-pencil: open the configuration page.
    Input values as shown below in the configuration screen.

    `Call type`: `Call static method`

    `class name`: `io.jans.service.cdi.util.CdiUtil`

    `method name`: `bean`

    `arguments`: `authServiceClass`

    `assign result to`: `authService`

    ![](../../../assets/agamalab-flow-passwd-editcdiutil.png)


4. Create `result object` Assignment block

    Next, we need to create an empty variable to store authentication results
    when we validate the credentials in subsequent steps. 

    Create a `Assignment` block after the `Get instance reference` call block and configure it as shown below.

    `Resultant variable`: `authResult`

    ![](../../../assets/agamalab-flow-passwd-edit-assignment.png)

5. Create Repeat block

    `Repeat` block represents the [Repeat](../../../agama/language-reference.md#repeat) instruction of Agama DSL.

    Repeat block creates a loop to iterate over certain steps(blocks). We will create a repeat loop that allows
    3 retries if the authentication fails.
    
    Create a Repeat block after the `Create result object` block 

    ![](../../../assets/agama-create-repeat.png)

    _Click on the newly created `Repeat` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agamalab-flow-passwd-edit-repeat.png)

6. Create a `Show login form` RRF block

    `RRF` block represents the [RRF](../../../agama/language-reference.md#rrf)
   instruction of Agama DSL. We will present the login form to the user using
   this node.

    Create an RRF block after the `Retry Authentication` block. 
    
    Remember that 
    the `Retry Authentication` is a `Repeat` type of block. It has two possible
    paths. 
    
    - One that originates from the colored dot on the bottom edge will be
    labeled as `repeat`. Execution of the blocks along this path will be
     repeated as configured in the `Repeat` block.
    - Second path originates from the colored dot on the right edge of the 
    `Repeat` block. This path is labeled as `Continue`. These blocks will be 
    executed after `repeat` path completes the execution.

    The new `RRF` block should be created on the `Repeat` path. 

    ![](../../../assets/check-repeat.png)

    Click on the newly created `RRF` block. Click :material-pencil:. Input values as shown below in the configuration
    screen. 

    `Template path`: `login.ftlh`

    `Data variable to inject`: `authResult`

    `Assign result to`: `creds`

    ![](../../../assets/agamalab-flow-passwd-edit-rrf.png)
    
    
    With this configuration, the RRF block will 

    - Present an input form to the user. We will build the `login.ftlh` in a [later section](#design-user-interface)
    - Capture and store user inputs like the user-name and password into the
     `creds` variable

    At this stage, let's save the flow using the `Save` button. This creates a new
    commit in your GitHub repository.

7. Create `validate credentials` call block

    Create a `Call` block to process the username and password received from the user (in RRF) and
    validate them. The result of the validation is stored in a variable.
    
    Create a new call block after the `Show login page` block.


    ![](../../../assets/agama-lab-create-cdiutil-instance.png)

    Click on the newly created `Call` block. Click :material-pencil:. 
    Configure it as shown below.

    `Call type`: `Call method on instance`

    `Variable name`: `authService`

    `Method name`: `authenticate`

    `Arguments`: `creds.username` and `creds.password`

    `Assign result to`: `authResult.success`

    ![](../../../assets/agamalab-flow-passwd-edit-cdiUtil-instance.png)

8. Create a conditional When block

    `When` block represents the [When](../../../agama/language-reference.md#conditionals-and-branching)
   instruction of Agama DSL.

    Create a conditional check using the `When` block to check if the 
    user credentials were successfully validated.

    Create a new `When` block after the `Validate credentials` block.

    ![](../../../assets/agama-lab-flow-password-create-when.png)

    _Click on the newly created `When` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-password-edit-when.png)

9. Create Finish blocks

     The `Finish` block represents the [Flow finish](../../../agama/language-reference.md#flow-finish) instruction of Agama DSL.

     We will need to create two finish blocks.

     - One finish block to terminate the flow when the authentication is successful.
     - Second finish block to terminate the flow when the authentication is still not successful after 3 repeated attempts.

     If the authentication was successful then the flow should finish and return the
     username. This will be achieved by adding a Finish block to the `Condition met` branch of the `When` block. And if authentication fails after 3
     attempts, we need another `Finish` block following the `Repeat` block.

     Drag and drop the colored dot on the lower vertex of the `When` block to create
     a `Condition met` branch. Create a `Finish` block on this branch.

     ![](../../../assets/agamalab-flow-passwd-create-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen_

     ![](../../../assets/agama-lab-flow-passwd-edit-finish.png)
     
    Let's create a second finish block that will terminate the flow when all 3
    authentication attempts have failed. 
    
    Create a `Finish` block on the `Continue` branch of the `Repeat` block. 

     ![](../../../assets/agamalab-flow-passwd-create-fail-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen and click `Save`._

     ![](../../../assets/agamalab-flow-passwd-edit-fail-finish.png)

     Save the flow using the `Save` button on flow canvas.

     The completed flow looks like below:

     ![](../../../assets/image-agama-lab-flow-quick-start-guide.png)

10. Check generated code

    The flow is translated into [Agama DSL](../../../agama/language-reference.md). Click the `Code` button to see the 
    generated code.

    ```
    Flow co.acme.password
        Basepath ""
    // Get authentication service class reference from the IDP
    authServiceClass = Call io.jans.as.server.service.AuthenticationService#class 
    // Get authentication service instance reference from the IDP
    authService = Call io.jans.service.cdi.util.CdiUtil#bean authServiceClass
    // Create empty object to store the authentication result
    authResult = {}
    // Retry 3 times to get the correct username and password 
    Repeat 3 times max
        // Load login page for username and password input
        creds = RRF "login.ftlh" authResult
        // Validate username and password with IDP
        authResult.success = Call authService authenticate creds.username creds.password
        // Check if the authentication was successful 
        When authResult.success is true
              // Authentication successful. Return the username.
              Finish creds.username
    // Max number of failed authentication attempts reached. return error and end the flow 
    it_yrwmc = {success:false, error: "Maximum authentication attempts reached. Authentication failed."}
    Finish it_yrwmc
    ```

### Design user interface

In the RRF configuration step, we mentioned `login.ftlh` to render the login page elements.
We need to add `login.ftlh` to the Agama project so that the flow can use it during the execution.
Use the steps below to create the page.

1. Create a template file

    _On the left project explorer menu, click on `web` > `New` > `Freemarker Template`_

    ![](../../../assets/agama-lab-flow-passwd-create-login-page.png)

    _Select `+ Create` under the `New Blank Template`_

    ![](../../../assets/agamalab-flow-passwd-select-template.png)

    _Give `Name` and `Description` as shown below and click `Create`_

    ![](../../../assets/agamalab-flow-passwd-edit-template.png)

2. Use the visual editor

    This opens a visual editor to create an Apache FreeMarker template. 
    Use this visual editor to create a template
    as per the need. For this article, we will use the code below.

    ![](../../../assets/agama-lab-flow-passwd-edit-template-2.png)

    _Click `Edit HTML`. This opens a text editor. Remove existing code in the editor and paste the code shown below._

    ```html
        <!DOCTYPE html>
    <html lang="en">
        <head>
      <meta charset="UTF-8">
      <meta http-equiv="X-UA-Compatible" content="IE=edge">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Username_Password_Form
      </title>
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/tailwindcss@2.2/dist/tailwind.min.css" type="text/css">
      </head>
      <body>
      <div class="md:h-screen bg-white relative flex flex-col justify-center items-center">
        <div class="md:border md:border-gray-300 bg-white md:shadow-lg shadow-none rounded p-10 align-middle">
        <div class="flex flex-col items-center space-y-3">
          <div class="flex">
          <img src="https://gluu.org/wp-content/uploads/2020/12/logo.png" id="i4xwu">
          </div>
          <span class="text-2xl font-semi-bold leading-normal">Form</span>
          <p class="leading-normal">Enter your Username and Password
          </p>
        </div>
        <form method="post" class="my-8">
          <div class="relative mb-2">
          <label for="username" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
            Username
          </label>
          <input type="text" id="username" name="username" autofocus="" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2 focus:border-green-700">
          </div>
          <div class="relative mb-2">
          <label for="password" class="label mt-2 ml-1 leading-tighter text-gray-600 text-base cursor-text">
            Password
          </label>
          <input type="password" id="password" name="password" autofocus="" required="" class="w-full rounded px-3 border border-gray-300 pt-2 pb-2 focus:border-green-700">
          </div>
          <div class="space-y-9">
          <div class="text-sm flex justify-center items-center">
            <button type="submit" class="py-2 px-6 rounded text-white btn bg-green-500 hover:bg-green-600">
            Submit
            </button>
          </div>
          </div>
        </form>
        </div>
      </div>
      </body>
    </html>
    ```

_Click `Save changes`_

![](../../../assets/agama-lab-flow-save-template.png)

_This will render the page in the visual editor_.

![](../../../assets/agama-lab-flow-render-template.png)

### Publish the project

At this point we have all the components of an Agama project ready, and they are 
stored in
the development repository. Next step is to publish the project into the 
release repository. Publishing will create the first release of this project 
in the release repository. Artifacts linked to the release in the
release repository contain the `.gama` package. This `.gama` package is used
to deploy the project in the IDP.

To publish the project, click on any of the files in the left project explorer, 
and click `Publish Project`.

![](../../../assets/agamalab-flow-passwd-publish-project.png)

Enter a desired version number for the first release and the web link to the 
GitHub release repository. Click `Publish`

![](../../../assets/agamalab-flow-passwd-publish-project-gh.png)

Upon successful publishing, the Agama Lab's `Releases` dashboard is shown. 
It lists the project that we released.

Click on the project name to go to the GitHub repository release page where 
`.gama` file has been released

![](../../../assets/agama-lab-flow-passwd-release-list.png)

Download the `.gama` file from here to deploy on to the Janssen Server.

![](../../../assets/agama-lab-flow-passwd-release-list-gh.png)

## Deploy Agama project

1. Download the `.gama` file on the Janssen Server.
   
2. Open Janssen Server [TUI](../../config-guide/config-tools/jans-tui/README.md) 
  and upload the `.gama` package using [these instructions](https://docs.jans.io/head/janssen-server/config-guide/auth-server-config/agama-project-configuration/#agama-project-screen)

3. Ensure that the project has been uploaded without any errors by refreshing
  the project listing after the upload. 

## Testing using Janssen Tarp

We will use the [Jans Tarp](https://github.com/JanssenProject/jans/tree/main/demos/jans-tarp), 
a browser extension based tool from the Janssen Project, 
to test this Agama project. 

- [Install Jans Tarp browser plug-in](https://github.com/JanssenProject/jans/tree/main/demos/jans-tarp#installation-in-browser)
- Follow [these instructions](https://github.com/JanssenProject/jans/tree/main/demos/jans-tarp#testing-using-jans-tarp) to test using Jans Tarp. Remember that the `ACR value` is the composed of fully qualified name
of your flow (in our case `co.acme.password`) preceded by `agama_`. That is
`agama_co.acme.password`.
- In a successful test run, the browser should prompt user for the user-name and 
  password using the same user-interface that we built in the previous steps.

  ![image](../../../assets/agama-login.png)

- Upon entering a valid user-name and password, the tarp should present a page
  with access token. 
  
  ![image](../../../assets/successful-tarp-auth-screen.png)

