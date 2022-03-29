Use the [assets](../assets) directory to store digital assets like images and diagrams that are used to support documentation for all modules. 

## Directory Structure
Directory structure under this directory should closely follow directory structure under [docs](../docs) directory. 

For example, documents located under `docs/user/howto` should place their supporting images\diagrams under `docs/assets/user/howto`.

## Images and Diagrams
Keep images and diagrams in separate directories as below. 
 - `images` : to hold screenshots and other images (Max size 1MB each, preferrably `.png`)
 - `diagrams` : to hold interaction diagrams, class diagrams, sequence diagram. (Preferrably as PDF)

For instance, images that support technical documentation for `jans-core` module should be located in `jans/docs/assets/technical/jans-core/images`

## File Naming Conventions 
Image/diagram file names should follow the pattern: `image/diagram`-`3 tag words`-`mmddyyyy`.`type`.

For example,
- A screenshot of IDE settings for developer workspace setup can be named `image-eclipse-ide-setting-04052022.png`
- A sequence diagram for user registration flow can be named `diagram-user-registration-sequence-03062022.pdf`
