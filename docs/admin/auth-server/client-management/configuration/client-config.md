---
tags:
  - administration
  - client
  - configuration
---

# Client Configuration

## ACR client configuration

There are 4 client configuration properties related to ACR:

- `default_acr_values` - string array, default acr values which are set when `acr_values` is missed in authorization request.
- `minimumAcrLevel` - integer value which sets minimum acr level.
- `minimumAcrLevelAutoresolve` - boolean value, if `false` and `minimumAcrLevel` is higher then current `acr_values` then reject request. If `true` - resolve acr according to either client's `minimumAcrPriorityList` or AS `auth_level_mapping`
- `minimumAcrPriorityList` - string array, enables client to specify the acr order of preference, rather then just the next lowest integer value

AS process properties in following order:
1. if `acr_values` is absent, set `acr_values` from `default_acr_values`
2. Otherwise if present, checking minimum acr level:
- check `minimumAcrLevel`, if current acr level is higher or equals to `minimumAcrLevel` then proceed request processing without changes
- if `minimumAcrLevel` is less then current acr level and `minimumAcrLevelAutoresolve=false` -> reject request (return bad request error)
- if `minimumAcrLevel` is less then current acr level and `minimumAcrLevelAutoresolve=true` -> pickup value from `minimumAcrPriorityList` or if it's empty take nearest acr value that satisfy `minimumAcrLevel` 

For example, given:
1. `minimumAcrLevel` = 14
1. `default_acr_values` = "basic"
1. `minimumAcrPriorityList` = ["u2f", "passkey", "usb_fido_key", "super_gluu"]
1. OP `auth_level_mapping` :
```
"auth_level_mapping": {
        "1": ["basic"],
        "5": ["otp"],
        "10": ["u2f"],
        "11": ["super_gluu"],
        "20": ["passkey"],
        "30": ["usb_fido_key"]      
    }
```

- if current `acr_values=u2f` and `minimumAcrLevelAutoresolve=false` -> request is rejected
- if current `acr_values=u2f` and `minimumAcrLevelAutoresolve=true` -> `acr_values` set to `acr_values=passkey` and request continue processing
- if current `acr_values=usb_fido_key` -> current acr is higher then minimum. Thus nothing to do.

If `minimumAcrPriorityList` is missing, then the AS can pick the next highest acr in the `auth_level_mapping`. In the example above, that would be `passkey`. 