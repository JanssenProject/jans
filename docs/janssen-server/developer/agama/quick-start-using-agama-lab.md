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

    ![](../../../assets/agama-loginpage.png)

2. Authenticate on the GitHub authentication page.

    ![](../../../assets/agama-lab-git-login.png)

3. Click on `Change Repository`, go to  `your repositories` > select your repositories name. Click `Select`
   
    ![](../../../assets/agama-lab-add-git-repo.png)


4. Project listing page will be shown. All the existing projects for your GitHub user are listed here. Projects can be
   created, edited, and deleted from the project listing page.

    ![](../../../assets/agamalab-project-page.png)

#### Create A New Project

1. To create a new project, click on the `New Project` button on the project listing page above.
   Enter the name and the description for the new project and click the `Create` button.

    ![](../../../assets/agamalab-new-proj.png)

2. This will create a new project and it'll be listed on the project listing page.

    ![](../../../assets/agamalab-project-listing.png)


#### Create The Authentication Flow


1. Create A Flow File

    _Click on :material-arrow-top-right:_

    The flow authoring view will open with a blank canvas. To start creating the flow, we need to create a `Flow File`. To
    do that,

    _Right-click on `code` and then select `New` > `Flow File`_

    ![](../../../assets/agamalab-new-flow.png)

    _Give name and description for the flow file and then click `Create`_

    ![](../../../assets/agamalab-new-flow-name.png)

    A newly created flow file has one block in it by default.

    ![](../../../assets/agamalab-flow-passwd-1.png)
    

    
    **todo**
    Drag and drop this :material-circle: then will be open the next node option or 
    to edit the existing block using :material-pencil:


2. Create _AuthenticationService_ Call block

    Drag and drop  end point of this :material-circle: at `Start` block then select `call`

    ![](../../../assets/agamalab-flow-passwd-create-call.png)

    A new `Call` block should appear with a link to `Start` block

    ![](../../../assets/agamalab-flow-passwd-new-call.png)

    _Click `Call` block and then click :material-pencil: to open the configuration screen._


    _Add configuration values as shown below._
   
    This will create a new instance of `AuthenticationService` class. This
    instance will be used to authenticate the user. The new instance will be stored in a variable called `authService`.
   
    ![](../../../assets/agamalab-flow-passwd-edit-call.png)

3.  Create _CdiUtil_ Call block

    To perform authentication we will also need a bean instance of `CdiUtil` class. This bean instance  
    takes `AuthenticationService` instance that we created in the previous step as an argument.

    After the `AuthenticationService` create new  `Call` node 
    

    ![](../../../assets/agama-create-cdiutil.png)

    _Click on the newly created `Call` block and by clicking :material-pencil: open the configuration page.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agamalab-flow-passwd-edit-cdiutil.png)

4. Create Assignment block

    Next, we need to create an empty variable that the flow will use in the future to store authentication results.
    
    Drag and drop  end point of this :material-circle: at `New Cdiutil Object` block then select `Assignment`

    ![](../../../assets/agama-create-assignment.png)

    _Click on the newly created `Assignment` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agamalab-flow-passwd-edit-assignment.png)

5. Create Repeat block

    `Repeat` block represents the [Repeat](../../../agama/language-reference.md#repeat) instruction of Agama DSL.

    Repeat block creates a loop to iterate over certain steps(blocks). We will create a repeat loop that allows
    3 retries if the authentication fails.
    
    Drag and drop  end point of this :material-circle: at `Result Object` block then select `Repeat`

    ![](../../../assets/agama-create-repeat.png)

    _Click on the newly created `Repeat` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agamalab-flow-passwd-edit-repeat.png)

6. Create An RRF block

    `RRF` block represents the [RRF](../../../agama/language-reference.md#rrf)
   instruction of Agama DSL.

    Drag and drop  end point of this :material-circle: at `Retry Authentication` block then select `RRF`

    ![](../../../assets/check-repeat.png)

    _Click on the newly created `RRF` block. Click :material-pencil:. Input values as shown below in the configuration
    screen_

    ![](../../../assets/agamalab-flow-passwd-edit-rrf.png)
    
    **todo**
    Since we have checked the `In Repeat Block` at the time of adding the `RRF` block, the `RRF` block
    as well as all the blocks that we add to the `RRF` block iterated blocks.

    _At this stage, let's save the flow. Click `Save` on the flow canvas._

7. Create a _CdiUtil_ Call block

    Create a `Call` block to process the username and password received from the user (in RRF) and
    validate them. The result of validation should be stored in a variable.
    
    Drag and drop  end point of this :material-circle: at `Login page` block then select `RRF`


    ![](../../../assets/agama-lab-create-cdiutil-instance.png)

    _Click on the newly created `Call` block. Click :material-pencil:. Input values as shown below in the configuration
    screen_

    ![](../../../assets/agamalab-flow-passwd-edit-cdiutil-instance.png)

8. Create An Assignment block

    In case of authentication failure, we want to show the username to the user while reentering the
    password on the web page. For this, we will save the username in a variable using the `Assignment` block.

    Drag and drop  end point of this :material-circle: at `Validate Credentials` block then select `Assignment`

    ![](../../../assets/agama-lab-create-assignment-uid.png)

    _Click on the newly created `Call` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agamalab-flow-passwd-edit-assignment-uid.png)

9. Create A Conditional When block

    `When` block represents the [When](../../../agama/language-reference.md#conditionals-and-branching)
   instruction of Agama DSL.

    Create a conditional check using the `When` block to check if the authentication (validated in
    `validate credentials` block) has been successful.

    Drag and drop end point of this :material-circle: at `Assignment` block then select `When`

    ![](../../../assets/agama-lab-flow-password-create-when.png)

    _Click on the newly created `When` block. Click :material-pencil:.
    Input values as shown below in the configuration screen_

    ![](../../../assets/agama-lab-flow-password-edit-when.png)

10. Create [finish]() blocks

     ***todo***
     The `Finish` block represents the [Flow finish](../../../agama/language-reference.md#flow-finish) instruction of Agama DSL.

     If the authentication was successful then the flow should finish and return the
     username. This will be achieved by adding a Finish block to the `When` block. And if authentication fails after 3
     attempts, we need another `Finish` block following the `Repeat` block.

     _Click on the `When` block. Click :material-plus-circle:. Click on `Condition met` and then click
     `Finish`_
      
    Drag and drop `Condition met` end point of this :material-circle: at `When` block then select `Finish`
    
    

     ![](../../../assets/agamalab-flow-passwd-create-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen_

     ![](../../../assets/agama-lab-flow-passwd-edit-finish.png)
     
    Drag and drop  end point of this :material-circle: at `Repeat` block then select `Finish`

     ![](../../../assets/agamalab-flow-passwd-create-fail-finish.png)

     _Click on the newly created `Finish` block. Click :material-pencil:.
     Input values as shown below in the configuration screen and click `Save`._

     ![](../../../assets/agamalab-flow-passwd-edit-fail-finish.png)

     Save the flow using the `Save` button on flow canvas.

     The completed flow looks like below:

     ![](../../../assets/agama-lab-flow-passwd-complete-flow.png)

11. Check Generated code

    Agama Lab flow gets translated in [Agama DSL](../../../agama/language-reference.md). Click the `Code` button to see the code
    generated by the flow.

```
Flow co.acme.password
     Basepath ""
// Create new  authentication service instance
authService = Call io.jans.server.service.authenticationService#class 
// Create new CdiUtil object
cdiUtil = Call io.jans.service.cdi.uti.CdUtil#bean authService
// Empty result object 
authResult = "{}"
// Retry 3 times to get correct username and password 
Repeat 3 times max
     // Loads the login page for username and password input 
     creds = RRF "login.ftlh" authResult
     // Validate username and password 
     authResult.success = Call cdiUtil authenticate creds.username creds.password
     // Keep the username in the user needs to retry authentication 
     authResult.uid = "creds.uername"
     When 
          // Return username in case of successful authentication 
          it_oghyk = {success:true, data: { userId: "as9233Qz", property1: "value1"}}
          Finish it_oghyk
     Otherwise
          RRF "__________" 
     Finish __________
// Max number of failed authentication attempts reached. Return False to end the flow
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

    ![](../../../assets/agamalab-flow-passwd-select-template.png)

    _Give `Name` and `Description` as shown below and click `Create`_

    ![](../../../assets/agamalab-flow-passwd-edit-template.png)

3. Use the visual editor

    This opens a visual editor to create a freemarker template. Use this visual editor to create a template
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

### Release The Project

To use the flow for authentication in the Janssen Server, the flow needs to be released. Agama Lab
releases the flow and the related artifacts (like template) in the form of a `.gama` file in the GitHub repository.

_To release the project, click on any of the files in the left project explorer, and click `Release Project`._

![](../../../assets/agamalab-flow-passwd-release-project.png)

_Enter a desired version number and click `Save`_

![](../../../assets/agamalab-flow-passwd-release-project-gh.png)

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

     ```bash title="Command"
     jans tui
     ```
    
3. Navigate to `Auth Server` > `Agama` > `Add a New Project` >`Upload Project`. Select the `.gama` file to upload.

    ![image](../../../assets/upload-gama-file.png)

4. Also can add a Community Project.

    ![image](../../../assets/select-community-agamaproject.png)

5. Navigate to `Auth Server` > `Agama` > `Add a New Project` > `Add a Community Project`. Select the `Agama` project.

    ![image](../../../assets/select-agama-project.png)


## Testing using Jans Tarp

* Setup Jans-Tarp. [Instructions](https://github.com/JanssenProject/jans/tree/main/demos/jans-tarp)
* Configuration to run Agama flow
* Add Your `server host`, `client expiry date`, `scopes` and click on Register client. It will register new OP Client on your Auth server.


![image](../../../assets/tarp-client-reg.png)

* Add `Acr values` and `Scope` and click on Trigger Auth Code flow
 
![image](../../../assets/authentication-flow-input.png)


* It will trigger agama flow and show login screen to you. You can use TUI to add testing user.

![image](../../../assets/agama-login.png)

* After successful auth, it will show you below screen

![image](../../../assets/successful-tarp-auth-screen.png)



## Test

1. [Setup](https://github.com/JanssenProject/jans/tree/v1.2.0/demos/jans-tent) Janssen Tent
2. Change the configuration as given below in `config.py`

     ```
     ACR_VALUES = "agama_co.acme.password.flow"
     ```

3. Run the Tent test by accessing it via the browser
