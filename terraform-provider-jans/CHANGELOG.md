# Changelog

## [1.1.5](https://github.com/JanssenProject/terraform-provider-jans/compare/v1.1.2...v1.1.5) (2024-09-11)


### Bug Fixes

* documentation 
* update API and sync with jans updates 

## [1.1.2](https://github.com/JanssenProject/terraform-provider-jans/compare/v1.1.0...v1.1.2) (2024-05-29)


### Bug Fixes

* update API and sync with jans updates 
* update docs 

## [1.1.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.8.2...v1.1.0) (2024-03-12)


### Features

* add KC and sync with upstream APIs 


### Bug Fixes

* oidc backchannel_user_code_parameter schema type 
* sync with upstream 
* update readme 

## [0.8.2](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.8.1...v0.8.2) (2023-11-09)


### Bug Fixes

* oidc backchannel_user_code_parameter schema type 

## [0.8.1](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.8.0...v0.8.1) (2023-10-26)


### Bug Fixes

* properly handle agama deployment autoconfigure 
* update provider to match latest API 
* updates in accordance to latest API changes 

## [0.8.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.7.3...v0.8.0) (2023-09-27)


### Features

* fixed app configuration, OIDC client, and attribute type to match latest API 


### Bug Fixes

* added new attributes to app config, client, custom scripts to match latest API version 

## [0.7.4](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.7.4...v0.7.4) (2023-09-28)


### Bug Fixes

* fixed app configuration, OIDC client, and attribute type to match latest API 


## [0.7.3](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.7.2...v0.7.3) (2023-07-28)


### Bug Fixes

* fixed dpop_use_nonce attribute type 
* fixed dpop_use_nonce attribute type 
* fixed dpop_use_nonce attribute type 

## [0.7.2](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.7.1...v0.7.2) (2023-07-27)


### Bug Fixes

* update README 

## [0.7.1](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.7.0...v0.7.1) (2023-06-05)


### Bug Fixes

* changed agama deployment file hash to be required, instead of computed 
* changed agama deployment file hash to be required, instead of computed 

## [0.7.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.6.0...v0.7.0) (2023-05-23)


### Features

* add missing attributes for fido2 app configuration updates 


### Bug Fixes

* fido2 config error and put operation 
* fixed request for sending binary data 
* update method name to match naming scheme 
* updated organisation mappings 

## [0.6.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.5.0...v0.6.0) (2023-05-15)


### Features

* added read-only ttl attribute to oidc client 
* aligned to latest auth-config-API 
* implemented handling of Agama deployments 

## [0.5.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.4.0...v0.5.0) (2023-04-17)


### Features

* change state handling of global configs that support patch updates 
* switched location type from ldap to db 

## [0.4.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.3.0...v0.4.0) (2023-03-16)


### Features

* added data source for plugins 

## [0.3.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.2.0...v0.3.0) (2023-03-15)


### Features

* added new resource for manaing api app config 

## [0.2.0](https://github.com/JanssenProject/terraform-provider-jans/compare/v0.1.0...v0.2.0) (2023-02-16)


### Features

* add initial janssen terraform provider 


### Bug Fixes

* 3 obsolete validations 
* added sorting of parameter array for app configuration 
* added support for nested slices as entity attributes 
* adjusted validations to match latest API definition 
* obsolete validations 
* remove debugging code 
* update attribute description to reflect new validation rules 
* update description of app config to include info on sort order 

## 0.1.0 (2023-02-08)


### Features

* add initial janssen terraform provider 
