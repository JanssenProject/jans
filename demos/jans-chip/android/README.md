# Jans Chip

## A  first party android mobile application that leverages dynamic client registration (DCR), DPoP access tokens.

[Demo Video](https://youtu.be/rqPewmESJb0)

### Steps followed in App for authentication

1. DCR with attestation
2. Execute Authorization Challenge Endpoint to get the Authorization Code
3. Generate DPoP Token using Authorization Code and DPoP header

### Workspace Setup

1. Clone `jans` monorepo.
   ```
    git clone https://github.com/JanssenProject/jans.git
   ```
2. Start Android Studio and open `{jans_monorep_path}\demos\jans-chip\android` of cloned jans monorepo. 
3. Press `ctrl` key twice on Android Studio to open `Run Anything` dialog.
4. Enter `gradle wrapper --gradle-version 8.0` and press enter key. This will generate gradle wrapper at `{jans_monorep_path}\demos\jans-chip\gradle\wrapper`. 
5. Build and run project on an emmulator (in Android Studio).
6. After launch add configuration endpoint of Janssen Server (with a trusted
   domain, not self-signed certificate) and desired scopes on the register screen
   to start testing.

## To-Dos

- Add FIDO authentication in app.



**Reference:**
- https://github.com/JanssenProject/jans/wiki/Mobile-DPoP-FIDO-Authn
- https://github.com/JanssenProject/jans/wiki/DPoP-Mobile-App-POC