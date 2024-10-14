---
tags:
  - administration
  - developer
  - agama
  - agama lab
---

# Quick Start Using Agama Lab

In this quick start guide, we will build, deploy and test a simple password-based authentication flow using
Agama and [Agama Lab](https://cloud.gluu.org/agama-lab).

[Agama Lab](https://cloud.gluu.org/agama-lab) is an online visual editor to build authentication flows. A flow
built on Agama Lab is packaged as a `.gama` project file. `.gama` package needs to be manually deployed on
Janssen Server where the Agama engine will execute the flow when an authentication request is received.

Major Steps involved in this process are:

- Create Agama project using Agama Lab
- Deploying `.gama` package on Janssen Server
- Testing the authentication flow

This guide covers these steps in the following sections.

## Prerequisites

- Janssen Server instance
- A public GitHub repository with at least one prior commit

## Create Agama Project

We will use [Agama Lab](https://cloud.gluu.org/agama-lab/) to create a simple username-password based user
authentication flow. This involves defining the authentication steps, designing the user interface to capture user
inputs, and lastly, releasing the flow as an Agama project.

### Defining The Authentication Flow

#### Log Into Agama Lab

1. Go to [Agama Lab](https://cloud.gluu.org/agama-lab) login page.

    ![](../../../assets/agama-lab-login.png)

2. Authenticate on the GitHub authentication page.

    ![](../../../assets/agama-lab-GH-login.png)

3. Input the name of the repository where Agama Lab should release the project artifacts. Click `Next`

    ![](../../../assets/agama-lab-add-gh-repo.png)

4. Project listing page will be shown. All the existing projects for your GitHub user are listed here. Projects can be
   created, edited, and deleted from the project listing page.

    ![](../../../assets/agama-lab-project-page.png)

#### Create A New Project

1. To create a new project, click on the `New Project` button on the project listing page above.
   Enter the name and the description for the new project and click the `Create` button.

    ![](../../../assets/agama-lab-new-proj.png)

2. This will create a new project and it'll be listed on the project listing page.

    ![](../../../assets/agama-lab-project-listing.png)


#### Create The Authentication Flow

1. Create A Flow File

    _Click on :material-arrow-top-right:_

    The flow authoring view will open with a blank canvas. To start creating the flow, we need to create a `Flow File`. To
    do that,

    _Right-click on `code` and then select `New` > `Flow File`_

    ![](../../../assets/agama-lab-new-flow.png)

    _Give name and description for the flow file and then click `Create`_

    ![](../../../assets/agama-lab-new-flow-name.png)

    A newly created flow file has one block in it by default.

    ![](../../../assets/agama-lab-flow-passwd-1.png)

    Clicking on the block will allow you to add further blocks using :material-plus-circle: or to edit the existing
    block using :material-pencil:.

2. Create _AuthenticationService_ Call block

    _Click on the block and then :material-plus-circle:. Then select `call`_

    ![](../../../assets/agama-lab-flow-passwd-create-call.png)

    A new `Call` block should appear with a link to `Start` block

    ![](../../../assets/agama-lab-flow-passwd-new-call.png)

    _Click `Call` block and then click :material-pencil: to open the configuration screen._

    _Add configuration values as shown below._
   
    This will create a new instance of `AuthenticationService` class. This
    instance will be used to authenticate the user. The new instance will be stored in a variable called `authService`.
   
    ![](../../../assets/agama-lab-flow-passwd-edit-call.png)

3.  Create _CdiUtil_ Call block

    To perform authentication we will also need a bean instance of `CdiUtil` class. This bean instance  
    takes `AuthenticationService` instance that we created in the previous step as an argument.

    _Click the `New Authentication Service` block and then click on :material-plus-circle:. Then click `Call`_

    ![](../../../assets/agama-lab-flow-passwd-create-cdiutil.png)

    _Click on the newly created `Call` block and by clicking :material-pencil: open the configuration page.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-cdiutil.png)

4. Create Assignment(s) block

    Next, we need to create an empty variable that the flow will use in the future to store authentication results.

    _Click on `New CdiUtil Object` and then click :material-plus-circle:. Select `Assignment(s)`_

    ![](../../../assets/agama-lab-flow-passwd-create-assignment.png)

    _Click on the newly created `Assign` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-assignment.png)

5. Create [repeat]() block

    `Repeat` block represents the [Repeat](../../../agama/language-reference.md#repeat) instruction of Agama DSL.

    Repeat block creates a loop to iterate over certain steps(blocks). We will create a repeat loop that allows
    3 retries if the authentication fails.

    _Click on the `Result Object` block and then click :material-plus-circle:. Select `Repeat`._

    ![](../../../assets/agama-lab-flow-passwd-create-repeat.png)

    _Click on the newly created `Repeat` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-repeat.png)

6. Create An RRF block

    `RRF` block represents the [RRF](../../../agama/language-reference.md#rrf)
   instruction of Agama DSL.

    _Click on the `Repeat` block. Click :material-plus-circle:. Check the `In Repeat Block` and then click on `RRF`._

    ![](../../../assets/agama-lab-flow-passwd-check-repeat.png)

    _Click on the newly created `Repeat` block. Click :material-pencil:. Input values as shown below in the configuration
    screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-rrf.png)

    Since we have checked the `In Repeat Block` at the time of adding the `RRF` block, the `RRF` block
    as well as all the blocks that we add to the `RRF` block iterated blocks.

    _At this stage, let's save the flow. Click `Save` on the flow canvas._

7. Create a _CdiUtil_ Call block

    Create a `Call` block to process the username and password received from the user (in RRF) and
    validate them. The result of validation should be stored in a variable.

    _Click on the `RRF` block. Click :material-plus-circle:. Click on `Call`._

    ![](../../../assets/agama-lab-flow-passwd-create-cdiutil-instance.png)

    _Click on the newly created `Call` block. Click :material-pencil:. Input values as shown below in the configuration
    screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-cdiutil-instance.png)

8. Create An Assignment block

    In case of authentication failure, we want to show the username to the user while reentering the
    password on the web page. For this, we will save the username in a variable using the `Assignment(s)` block.

    _Click on the `Call` block. Click :material-plus-circle:. Click on `Assignment(s)`._

    ![](../../../assets/agama-lab-flow-passwd-create-assignment-uid.png)

    _Click on the newly created `Call` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-assignment-uid.png)

9. Create A Conditional When block

    `When` block represents the [When](../../../agama/language-reference.md#conditionals-and-branching)
   instruction 
   of Agama DSL.

    Create a conditional check using the `When` block to check if the authentication (validated in
    `validate credentials` block) has been successful.

    _Click on `Assignment(s)` block. Click :material-plus-circle:. Click on `When`._

    ![](../../../assets/agama-lab-flow-passwd-create-when.png)

    _Click on the newly created `When` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-passwd-edit-when.png)

10. Create [finish]() blocks

     The `Finish` block represents the [Flow finish](../../../agama/language-reference.md#flow-finish) instruction of Agama DSL.

     If the authentication was successful then the flow should finish and return the
     username. This will be achieved by adding a Finish block to the `When` block. And if authentication fails after 3
     attempts, we need another `Finish` block following the `Repeat` block.

     _Click on the `When` block. Click :material-plus-circle:. Click on `Condition met` and then click
     `Finish`_

     ![](../../../assets/agama-lab-flow-passwd-create-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen_

     ![](../../../assets/agama-lab-flow-passwd-edit-finish.png)

     _Click on the `Repeat` block. Click :material-plus-circle:. Click `Finish`_

     ![](../../../assets/agama-lab-flow-passwd-create-fail-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen and click `Save`._

     ![](../../../assets/agama-lab-flow-passwd-edit-fail-finish.png)

     Save the flow using the `Save` button on flow canvas.

     The completed flow looks like below:

     ![](../../../assets/agama-lab-flow-passwd-complete-flow.png)

11. Check Generated code

    Agama Lab flow gets translated in [Agama DSL](../../../agama/language-reference.md). Click the `Code` button to see the code
    generated by the flow.

      ```
       Flow co.acme.password
        Basepath ""
       authService = Call io.jans.as.server.service.AuthenticationService#class
       cdiUtil = Call io.jans.service.cdi.util.CdiUtil#bean authService
       authResult = {}
       Repeat 3 times max
         creds = RRF "login.ftlh" authResult
         authResult.success = Call cdiUtil authenticate creds.username creds.password
         authResult.uid = creds.username
         When authResult.success is true
           Finish authResult.uid
       Finish false
      ```

### Design User Interface

In the RRF configuration step, we mentioned `login.ftlh` to render the login page elements.
We need to add `login.ftlh` to the Agama project so that the flow can use during the flow execution.
Use the steps below to create the page.

1. Create a template file

    _On the left project explorer menu, click on `web` > `New` > `Freemarker Template`_

    ![](../../../assets/agama-lab-flow-passwd-create-login-page.png)

    _Select `+ Create` under the `New Blank Template`_

    ![](../../../assets/agama-lab-flow-passwd-select-template.png)

    _Give `Name` and `Description` as shown below and click `Create`_

    ![](../../../assets/agama-lab-flow-passwd-edit-template.png)

3. Use the visual editor

    This opens a visual editor to create a freemarker template. Use this visual editor to create a template
    as per the need. For this article, we will use the code below.

    ![](../../../assets/agama-lab-flow-passwd-edit-template-2.png)

    _Click `Edit HTML`. This opens a text editor. Remove existing code in the editor and paste the code shown below._

    ```html
    <!doctype html>
    <html xmlns="http://www.w3.org/1999/xhtml">
       <head>
         <title> Jans Agama Basic Auth flow </title>
       </head>
       <body>
   
         <h2>Welcome</h2>
         <hr />
         
         [#if !(success!true)]
           <p class="fs-6 text-danger mb-3">${msgs["login.errorMessage"]}</p>
         [/#if]
           
         <hr />
         <form method="post" enctype="application/x-www-form-urlencoded">
           
           <div>
               Username: <input type="text" class="form-control" name="username" id="username" value="${uid!}" required>
           </div>
           
           <div>
               Password: <input type="password" class="form-control" id="password" name="password">
           </div>
           
           <div>
               <input type="submit" class="btn btn-success px-4" value="Login">
           </div>
         </form>
       </body>
       <style>
           input {
               border: 1px solid #000000;
           }
       </style>
    </html>
    ```

    _Click `Save changes`_

    ![](../../../assets/agama-lab-flow-passwd-save-template.png)

    _This will render the page in the visual editor_.

    ![](../../../assets/agama-lab-flow-passwd-render-template.png)

### Release The Project

To use the flow for authentication in the Janssen Server, the flow needs to be released. Agama Lab
releases the flow and the related artifacts (like template) in the form of a `.gama` file in the GitHub repository.

_To release the project, click on any of the files in the left project explorer, and click `Release Project`._

![](../../../assets/agama-lab-flow-passwd-release-project.png)

_Enter a desired version number and click `Save`_

![](../../../assets/agama-lab-flow-passwd-release-project-gh.png)

Upon successful release, Agama Lab `Releases` dashboard is shown. It lists all projects that are released.

_Click on the project name to go to the GitHub repository release page where `.gama` file has been released_

![](../../../assets/agama-lab-flow-passwd-release-list.png)

Download the `.gama` file from here to deploy on to the Janssen Server.

![](../../../assets/agama-lab-flow-passwd-release-list-gh.png)

## Deploy Agama Project

!!! Note
Please ensure that Agama engine and scripts are [enabled](../agama/engine-bridge-config.md#availability) in Janssen
Server deployment

1. Download the `.gama` file from the GitHub repository
2. Open [TUI](../../config-guide/config-tools/jans-tui/README.md) using following commands on Janssen Server

     ```
     cd /opt/jans/jans-cli
     python3 jans_cli_tui.py
     ```

3. Navigate to `Auth Server` > `Agama` > `Upload Project`. Select the `.gama` file to upload.

## Test

1. [Setup](https://github.com/JanssenProject/jans/tree/main/demos/jans-tent) Janssen Tent
2. Change the configuration as given below in `config.py`

     ```
     ACR_VALUES = "agama_co.acme.password.flow"
     ```

3. Run the Tent test by accessing it via the browser
