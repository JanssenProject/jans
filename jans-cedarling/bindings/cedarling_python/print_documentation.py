from cedarling_python import AuthzConfig
import inspect


def print_inspect(type_value):
    '''
        print the signature and doc string for a given type
    '''
    # we add additional newlines to make it more readable.
    print(f"show signature of type: {type_value.__name__}:\n")
    print(inspect.signature(AuthzConfig))
    print(f"show documentation string of type: {type_value.__name__}:\n")
    print(AuthzConfig.__doc__)
    print("\n")


print_inspect(AuthzConfig)
