# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

import inspect
from types import ModuleType
import cedarling_python


# script to show the signature and documentation string for a python cedarling bindings


def is_module(variable):
    '''
    Check if a variable is a module
    '''
    return isinstance(variable, ModuleType)


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

    print(f"Show signature of type: {type_value.__name__}: {inspect.signature(type_value)}")

    methods_list = [method for method in dir(type_value) if callable(
        getattr(type_value, method)) and not method.startswith("_")]

    for i, method in enumerate(methods_list):
        if i != 0:
            print()
        signature = inspect.signature(getattr(type_value, method))
        print(f"Signature of method {type_value.__name__}.{method}:{signature}")
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


# Add more descriptive docstrings for data_errors_ctx error classes
DATA_ERRORS_CTX_DOCS = {
    "DataErrorCtx": "Base exception for errors encountered during data operations in Cedarling context storage.",
    "InvalidKey": "Raised when an invalid (e.g., empty) key is provided to the context data store. This typically means the key argument was missing or empty.",
    "KeyNotFound": "Raised when a requested key is not found in the context data store. This usually means the key does not exist or has expired.",
    "SerializationError": "Raised when there is a failure serializing or deserializing data for storage or retrieval in the context data store.",
    "StorageLimitExceeded": "Raised when an operation would exceed the maximum allowed storage size for the context data store.",
    "TTLExceeded": "Raised when a requested time-to-live (TTL) value exceeds the maximum allowed by the context data store.",
    "ValueTooLarge": "Raised when a value is too large to be stored in the context data store, exceeding the allowed size limit."
}


def print_doc(type_value: any, module_name: str | None = None):
    '''
        this is a helper function show to doc string for a given type
    '''
    # Patch docstrings for data_errors_ctx error classes
    if module_name and module_name.endswith("data_errors_ctx"):
        doc_override = DATA_ERRORS_CTX_DOCS.get(type_value.__name__)
        if doc_override:
            type_value.__doc__ = doc_override

    message = fix_newlines(type_value.__doc__)
    message_lines = message.split("\n")
    if len(message_lines) != 0:
        # check if in first line we have name of type
        if not type_value.__name__ in message_lines[0]:
            # add name of type to the first line
            head_line = f"# {type_value.__name__}"
            message_lines = [head_line] + message_lines
    message = "\n".join(message_lines)

    if module_name is not None:
        message = message.replace(type_value.__name__, "{}.{}".format(
            module_name, type_value.__name__))
    print(message)
    print("___\n")


def print_module_doc(module: ModuleType):
    attrs = [attr for attr in dir(
        module) if not attr.startswith("_") and attr is not None]
    attrs.sort()

    for attr in attrs:
        print_doc(getattr(module, attr), module.__name__)


def filter(name):
    """
    Filter attribute names of `cedarling_python` library.
    """
    if name.startswith("_") or name is None:
        return False

    if name == "cedarling_python":
        return False

    return True


attr_names = [attr for attr in dir(cedarling_python) if filter(attr)]
attr_names.sort()

types = [getattr(cedarling_python, attr_name) for attr_name in attr_names]

if __name__ == "__main__":

    print_header()
    for t in types:
        if is_module(t):
            print_module_doc(t)
        else:
            print_doc(t)
