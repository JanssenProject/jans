# Jans Chip

## A  first party android mobile application that leverages dynamic client registration (DCR), DPoP access tokens.

[Demo Video](https://www.loom.com/embed/66e145e3bba4406ebda53715168ca8f9?sid=e946f580-587e-4c55-8ea8-3845d6ae4ce9)


### Steps followed in App for authentication

#### Enrolment

![](./docs/enrolment.png)

#### Authentication

![](./docs/authentication.png)

### Prerequisite

1. A Running Janssen Auth server and Janssen FIDO server.
2. SSA generated from the Janssen Auth server with `authorization_code` `grant_types`.
 
### Auth Challenge Script

Add following [Auth Challenge Script](./docs/authChallengeScript.java) in Jans Server (before using the App) with following details.

- **Name of Script** : passkey_auth_challenge
- **Script Type** : Authorization Challenge
- **Programming Language** : Java
- **Location Type**: Database
- Add `fido2_server_uri` custom property with https://{fido_server_url} as value. Replace `{fido_server_url}` with fido server hostname.

### Workspace Setup

1. Clone `jans` monorepo.
   ```
    git clone https://github.com/JanssenProject/jans.git
   ```
2. Start Android Studio and open `{jans_monorep_path}\demos\jans-chip\android` of cloned jans monorepo. 
3. Press `ctrl` key twice on Android Studio to open `Run Anything` dialog.
4. Enter `gradle wrapper --gradle-version 8.0` and press enter key. This will generate gradle wrapper at `{jans_monorep_path}\demos\jans-chip\gradle\wrapper`.
5. Add SSA from Janssen Auth server to SSA field in `demos/jans-chip/android/app/src/main/java/io/jans/chip/utils/AppConfig.kt`.
5. Build and run project on an emmulator (in Android Studio).


**Reference:**
- https://github.com/JanssenProject/jans/wiki/Mobile-DPoP-FIDO-Authn
- https://github.com/JanssenProject/jans/wiki/DPoP-Mobile-App-POC