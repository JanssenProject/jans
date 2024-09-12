# Python Cedarling
Example poetry application that works with the `cedarling_python` python binding.

## Using the `cedarling_python` Python Binding with `poetry`

### Using `poetry` from stratch

1. **Set Up Your Project with Poetry**
    If you haven't already initialized your project, create a new project or initialize an existing one with Poetry:

    ```bash
    poetry init
    ```

    This command will help you set up the `pyproject.toml` file.

1. Compile `whl` file for cedarling_python.
    Watch to the `cedarling_python` folder and in [README.md](../../bindings\cedarling_python\README.md) file.
    You need to read the `Prerequisites` and `Only Building the Library` sections. And then follow the steps.

    After executing the last command `maturin build` you will see in the console something like this:
    ```
    ...
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 20.27s
    📦 Built wheel for CPython 3.11 to D:\upwork\jans\jans-lock\cedarling\target\wheels\cedarling_python-0.1.0-cp311-none-win_amd64.whl
    ```
    Where `D:\upwork\jans\jans-lock\cedarling\target\wheels\cedarling_python-0.1.0-cp311-none-win_amd64.whl` is the path of the `whl` file.

1. **Install Dependencies **
    If you need to install dependency of cedarling_python for your poetry project, you can do so with Poetry:

    ```bash
    poetry add <path_to_wheel_file>
    ```
    Where `<path_to_wheel_file>` is the path of the `whl` file from previous step.

1. Add your python program and execute.
    For example:
    ```
    poetry run python .\pythonpoetry.py
    ```

### Using with this example


1. Compile `whl` file for cedarling_python.
    Watch to the `cedarling_python` folder and in [README.md](../../bindings\cedarling_python\README.md) file.
    You need to read the `Prerequisites` and `Only Building the Library` sections. And then follow the steps.

    After executing the last command `maturin build` you will see in the console something like this:
    ```
    ...
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 20.27s
    📦 Built wheel for CPython 3.11 to D:\jans\jans-lock\cedarling\target\wheels\cedarling_python-0.1.0-cp311-none-win_amd64.whl
    ```
    Where `D:\jans\jans-lock\cedarling\target\wheels\cedarling_python-0.1.0-cp311-none-win_amd64.whl` is the path of the `whl` file.

1. **Install Dependencies **
    If you need to install dependency of cedarling_python for your poetry project, you can do so with Poetry:

    ```bash
    poetry add <path_to_wheel_file>
    ```
    Where `<path_to_wheel_file>` is the path of the `whl` file from previous step.

1. Add your python program and execute.
    For example:
    ```
    poetry run python .\pythonpoetry.py
    ```
