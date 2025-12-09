from .cedarling_python import *

# The authorize_errors submodule is created in Rust code and attached
# to the cedarling_python module. We expose it here for better IDE support.
# At runtime, cedarling_python.authorize_errors will be available.
# For static analysis, the authorize_errors.py stub provides type hints.
