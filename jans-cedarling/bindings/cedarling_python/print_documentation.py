from cedarling_python import AuthzConfig, MemoryLogConfig, OffLogConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig

import inspect

# script to show the signature and documentation string for a python cedarling bindings


def print_inspect(type_value):
    '''
        print the signature and doc string for a given type
    '''
    # we add additional newlines to make it more readable.
    print(f"Show signature of type: {type_value.__name__}:\n")
    print(inspect.signature(type_value))
    print(f"Show documentation string of type: {type_value.__name__}:\n")
    print(type_value.__doc__)
    print("\n")


types = [AuthzConfig, MemoryLogConfig, OffLogConfig,
         StdOutLogConfig, PolicyStoreSource, PolicyStoreConfig, BootstrapConfig]
for t in types:
    print_inspect(t)
