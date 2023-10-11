This is a person authentication module for oxAuth that enables user to forward authorization endpoint with a different acr_values as specified in the Custom property Key.

1) new_acr_value - It's mandatory property. It's the new acr_value where user will be routed. Please make sure acr_value where user will be redirected is enabled and shown avaialble in acr_values_supported /.well-known/openid-configuration.
