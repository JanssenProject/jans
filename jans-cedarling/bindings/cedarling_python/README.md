# cedarling_python 🐍

This project uses `maturin` to create a Python library from Rust code. Follow the steps below to install and build the library.

## Prerequisites

1. (Optional) Install build tools (for Linux users)
   
   Install the `build-essential` package or its equivalent packages using your distribution's package manager.

   Ubuntu/Debian:

   ```
   sudo apt install build-essential
   ```

   Arch Linux:

   ```
   sudo pacman -S base-devel
   ```

   RHEL/CentOS/Fedora:

   ```
   sudo yum install gcc gcc-c++ make openssl-dev
   ```

1. Ensure Rust is installed:
   Verify Rust installation by running:

   ```
   cargo --version
   ```

   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install).

1. Set up a virtual environment:
   - Install `venv` for your platform by following the [instructions here](https://virtualenv.pypa.io/en/latest/installation.html).
   - Create a virtual environment:

     ```bash
     python3 -m venv venv
     ```

1. Activate the virtual environment:
   Follow the [instructions here](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment) to activate the virtual environment.

   Example for Linux/macOS:

   ```bash
   source venv/bin/activate
   ```

   Example for Windows:

   ```bash
   .\venv\Scripts\activate
   ```

1. Install `maturin`

   ```
   pip install maturin
   ```
   For Linux, install patchelf dependency:
   ```
   pip install maturin[patchelf]
   ```

1. Clone the repository:
   ```
   git clone https://github.com/JanssenProject/jans.git
   ```

1. Navigate to the `cedarling_python` folder:

   ```
   cd jans/jans-cedarling/bindings/cedarling_python
   ```

1. Build and install the package:
   Build the Rust crate and install it into the virtual environment:

   ```
   maturin develop --release
   ```

1. Verify installation:
   Check that the library is installed by listing the installed Python packages:

   ```
   pip list
   ```

   You should see `cedarling_python` listed among the available packages.

1. Read documentation
  After installing the package you can read the documentation from python using the following command:

   ```bash
   python -m pydoc cedarling_python
   ```

## Running a Python Script

To verify that the library works correctly, you can run the provided `example.py` script. Make sure the virtual environment is activated before running the script:

```bash
CEDARLING_LOCAL_POLICY_STORE=example_files/policy-store.json python example.py
```

## Building the Python Library

If you only want to build the library without installing it in the Python environment, follow these steps:

1. Complete the [prerequisites](#Prerequisites)

1. Navigate to the `cedarling_python` folder:

   ```bash
   cd jans/jans-cedarling/bindings/cedarling_python
   ```

1. Build the crate:
   To build the library:

   ```bash
   maturin build --release
   ```

## Python types definitions

  The python types definitions are available in the `PYTHON_TYPES.md` file. Or by clicking [here](PYTHON_TYPES.md).
  Also after installing the library you can get same information using:

  ```bash
  python -m pydoc cedarling_python
  ```

## Testing the Python bindings

  We use `pytest` and `tox` to create reproduceable environments for testing.

### Run test with `pytest`  

  To run the tests, with `pytest`:
  
  1. Make sure that you have installed the `cedarling_python` package in your virtual environment or system.
  1. Install `pytest`:

     ```bash
     pip install pytest
     ```

  1. Make sure that you are in the `jans/jans-cedarling/bindings/cedarling_python/` folder.
  1. Run the following command:

     ```bash
     pytest
     ```

     Or run `pytest` without capturing the output:

     ```bash
     pytest -s
     ```

  1. See the results in the terminal.
  
### Run test with `tox`

  1. Ensure that you installed rust compiler and toolchain. You can install it by following the official [rust installation guide](https://www.rust-lang.org/tools/install).

  1. Ensure tox is installed:
  You can install tox in your environment using pip:

     ```bash
     pip install tox
     ```

  1. Make sure that you are in the `jans/jans-cedarling/bindings/cedarling_python/` folder.
  1. Run the following command:

     ```bash
     tox
     ```

  1. See the results in the terminal.
