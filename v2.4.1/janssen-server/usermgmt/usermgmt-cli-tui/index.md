# Jans CLI/TUI User Management

In this document we will cover managing people in the Jans Server's Jans CLI and Jans TUI.

## Manage data in Jans TUI

We can easily add a user using Jans TUI. To do that, run the TUI using `jans tui` for example, and click on the `Users` tab.

Let's see at a glance user attributes.

| Attributes                            |
| ------------------------------------- |
| Username                              |
| Password                              |
| First Name                            |
| Middle Name                           |
| Last Name                             |
| Display Name                          |
| Email                                 |
| Active                                |
| Nickname                              |
| CIBA Device Registration Token        |
| CIBA User code                        |
| Locale                                |
| Website URL                           |
| IMAP Data                             |
| jansAdminUIRole                       |
| Enrollment code                       |
| User Permission                       |
| Preferred Language                    |
| Profile URL                           |
| Secret Question                       |
| Email Verified                        |
| Birthdate                             |
| Time zone info                        |
| Phone Number verified                 |
| Preferred Username                    |
| TransientId                           |
| PersistentId                          |
| Country                               |
| Secret Answer                         |
| OpenID Connect JSON formatted address |
| User certificate                      |
| Organization                          |
| Picture URL                           |

Let's add an user by selecting `Add Users` tab. Initially we can provide bellow attributes value,

We can add extra claims simple by slecting `Add Claim`

Finally `save` and exit. We will get an unique `inum`

## Change User Password

No chance to recover user password, but you can change. To change password of a user navigate/or search user and press key **p** when the target user is higlighted. In the figure below, passowrd of user **sakamura** is being changed.

Once you write new password (it will be displayed while you type), go to button `< Save >` and press Enter.

## Manage User FIDO Devices

To view and manage users registered FIDO devices, first navigate/or search user and press key **f** on the keyboard. If user has any registered FIDO device, a popup will appears as in image below:

You can veiw details of a device by pressing Enter. To delete a device press key **d**, you will be prompted for confirmation.
