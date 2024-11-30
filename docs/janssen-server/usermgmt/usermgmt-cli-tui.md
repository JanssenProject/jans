---
tags:
  - administration
  - user management
  - cli
  - tui
---

## Local User Management

In this document we will cover managing people in the Jans Server's Jans CLI and Jans TUI.


## Manage data in Jans TUI

We can easily add a user using Jans TUI. To do that, run the TUI using `jans tui` for example, and click on the `Users` tab.

![user-tab](https://github.com/JanssenProject/jans/assets/43112579/d94916ff-82c6-4d64-bd3a-9b13f3a5038d)

Let's see at a glance user attributes.

|Attributes|Description|
|---|---|
|Username|...| 
|Password |...|
|First Name|...|
|Middle Name|...|
|Last Name|...|
|Display Name|...|
|Email |...|
|Active|...|
|Nickname|...|
|CIBA Device Registration Token|...|
|CIBA User code|...|
|Locale|...|      
|Website URL|...| 
|IMAP Data|...|   
|jansAdminUIRole|...|
|Enrollment code|...|
|User Permission|...|
|Preferred Language|...|
|Profile URL|...|
|Secret Question|...|
|Email Verified|...|
|Birthdate|...|   
|Time zone info|...|
|Phone Number verified|...|
|Preferred Username|...|
|TransientId|...| 
|PersistentId|...|
|Country|...|     
|Secret Answer|...|
|OpenID Connect JSON formatted address|...|
|User certificate|...|
|Organization|...|
|Picture URL|...| 


Let's add an user by selecting `Add Users` tab. Initially we can provide bellow attributes value,

![add-user](https://github.com/JanssenProject/jans/assets/43112579/9f124b19-de4c-401f-9d7b-ac4b32c78163)

 We can add extra claims simple by slecting `Add Claim`

![add-claim](https://github.com/JanssenProject/jans/assets/43112579/97673b9e-4f45-4af3-869a-dfb86a8e972f)

Finally `save` and exit. We will get an unique `inum`


## Change User Password
No chance to recover user password, but you can change.
To change password of a user navigate/or search user and press key **p** when the target user is higlighted.
In the figure below, passowrd of user **sakamura** is being changed.

[Change User Password](../../assets/tui-user-change-password.png)

Once you write new password (it will be displayed while you type), go to button `< Save >` and press Enter.

## Manage FIDO Devices
To view and manage users registered FIDO devices, first navigate/or search user and press key **f** on the keyboard.
If user has any registered FIDO device, a popup will appears as in image below:

[User FIDO Devices](../../assets/tui-ser-fido-device-list.png)

You can veiw details of a device by pressing Enter. To delete a device press key **d**, you will be
prompted for confirmation.


## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
