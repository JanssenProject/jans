# Jans Chip

## A  first party android mobile application that leverages dynamic client registration (DCR), DPoP access tokens.

[Demo Video](https://youtu.be/rqPewmESJb0)

### Steps followed in App for authentication

1. DCR with attestation
2. Execute Authorization Challenge Endpoint to get the Authorization Code
3. Generate DPoP Token using Authorization Code and DPoP header

### Workspace Setup

- Setup workspace using Android Studio IDE
- Import `android` directory as project
- IDE should automatically detect `app` as run configuration and now it is ready
to be launched on an emmulator
- After launch add configuration endpoint of Janssen Server (with a trusted
domain, not self-signed certificate) and desired scopes on the register screen 
to start testing

## To-Dos

- Add FIDO authentication in app.



**Reference:**
- https://github.com/JanssenProject/jans/wiki/Mobile-DPoP-FIDO-Authn
- https://github.com/JanssenProject/jans/wiki/DPoP-Mobile-App-POC