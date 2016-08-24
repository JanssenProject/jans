# FIDO UAF Authenticator

The UAF allows applications to take advantage of the security capabilities of modern devices such as fingerprint 
biometrics, iris recognition, voice biometrics and others. It provides a unified infrastructure that allows you to integrate these capabilities in a simple manner to enable authentication that is both more user friendly and more secure than password

## Typical UAF architecture

This is generic diagram which provides UAF architecture overview. There are 2 parts. One part contains components that resides on the user’s device. Another part is RP and UAF server. The typical gateway between these 2 parts is mobile browser with UAF plugin. The RP should provides proxy capabilities to deliver messages from mobile browser plugin to UAF server.

![Typical UAF design](../img/typical_uaf_architecture.png)

It's not very convinient when RP is an second device. Non Nok offer OOB API which simplify UAF integration in this case.   

## Device integration models

This is not part of UAF authentication script but shows modular architecture of UAF mobile authentication part.

Some devices will feature a preloaded UAF Client and one or more UAF ASMs. In some cases, 
devices may only feature a preloaded UAF ASM, rather than both a UAF Client and ASM. Older 
legacy devices may feature neither a UAF Client nor a UAF ASM. Nevertheless the App SDK 
can support each of these three scenarios illustrated below from the same mobile application. 

![Typical UAF design](../img/uaf_device_integration_models.png)


## Integration with oxAuth
 