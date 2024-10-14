from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig
from cedarling_python import Cedarling

import inspect

# script to show the signature and documentation string for a python cedarling bindings


def print_inspect(type_value):
    '''
        this is a helper function to show the signature and doc string for a given type
        is used only to validate is documentation and signature from the python side
    '''
    # we add additional newlines to make it more readable.
    print("___")
    print(f"Show documentation string of type: {type_value.__name__}:\n")
    print(type_value.__doc__)
    print("\n")

    print(f"Show signature of type: {type_value.__name__}: {
          inspect.signature(type_value)}")

    methods_list = [method for method in dir(type_value) if callable(
        getattr(type_value, method)) and not method.startswith("_")]

    for i, method in enumerate(methods_list):
        if i != 0:
            print()
        signature = inspect.signature(getattr(type_value, method))
        print(f"Signature of method {
              type_value.__name__}.{method}:{signature}")
        doc = getattr(type_value, method).__doc__
        if doc is not None:
            print(f"documentation: {doc}")


def print_header():
    print('''
# Cedarling Python bindings types documentation

This document describes the Cedarling Python bindings types.  
Documentation was generated from python types.
''')


def fix_newlines(str_value):
    '''
    make newline in markdown correct
    '''
    return str_value.replace("\n:", "  \n:")


def print_doc(type_value):
    '''
        this is a helper function show to doc string for a given type
    '''
    print(fix_newlines(type_value.__doc__))
    print("___\n")


types = [MemoryLogConfig, DisabledLoggingConfig,
         StdOutLogConfig, PolicyStoreSource, PolicyStoreConfig, BootstrapConfig,
         Cedarling]

if __name__ == "__main__":
    print_header()
    for t in types:
        # print_inspect(t)
        print_doc(t)
