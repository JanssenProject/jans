## Basic Lock Account

Script contents [here](https://github.com/JanssenProject/jans/jans-linux-setup/static/extension/person_authentication/BasicLockAccountExternalAuthenticator.py)

This is a person authentication script for jans-auth-server which does basic authentication.
It looks user account after specified number of unsuccessful login attempts.

This module has 2 properties:

1) invalid_login_count_attribute - Specify attribute where script stores count of invalid number of login attemps
   Default value: jansCountInvalidLogin

2) maximum_invalid_login_attemps - Specify how many times user can enter invalid password before application will lock account
   Allowed values: integer value greater that 0
   Example: 3
   Default value: 3
3) lock_expiration_time - Specify the time in seconds when lock will be expired
   Default value: 180

While an account is locked, `invalid_login_count_attribute` holds `locked:<epoch millis>:<jansVersion>` instead of a
count, where `jansVersion` is the user version the lock wrote. The script only unlocks inactive accounts whose marker
and version are unchanged since the lock, so accounts disabled or edited by an administrator stay locked.

The script's user writes are compare-and-set on `jansVersion`: a counter, lock or unlock write that races another
write re-reads the user and decides again, so the script never overwrites a concurrent change. Janssen's own user
writers (auth server, Config API, SCIM, Casa, Jans Link) all advance `jansVersion`. Custom entity classes or direct
database updates on `jansPerson` must advance it too, or unlock cannot see their changes. Counter and lock writes fall back to a plain update on backends without versioned updates;
unlock does not, so such accounts must be reactivated by an administrator.

Upgrade note: accounts locked by a previous version of this script have no marker and are not unlocked when their
lock expires. An administrator must reactivate them.
