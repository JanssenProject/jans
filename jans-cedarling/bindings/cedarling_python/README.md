# cedarling_python 🐍

This project uses `maturin` to create a Python library from Rust code. Follow the steps below to install and build the library.

## Prerequisites and Installing the Python Library

1. (Optional) Install build tools (for Linux users): You may need to install essential build tools by running:

   ```bash
   sudo apt install build-essential
   ```

1. **Install `maturin`:**
   We recommend using `pipx` to install `maturin`. First, ensure `pipx` is installed by following the [official instructions](https://pipx.pypa.io/stable/).

   To install `maturin` using `pipx`:

   ```bash
   pipx install maturin
   ```

1. **Ensure Rust is installed:**
   Verify Rust installation by running:

   ```bash
   cargo --version
   ```

   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install).

1. **Clone the repository**:
   Ensure the repository is loaded into your directory.

1. **Set up a virtual environment**:
   - Install `venv` for your platform by following the [instructions here](https://virtualenv.pypa.io/en/latest/installation.html).
   - Create a virtual environment:

     ```bash
     python3 -m venv venv
     ```

1. **Activate the virtual environment**:
   Follow the [instructions here](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment) to activate the virtual environment.

   Example for Linux/macOS:

   ```bash
   source venv/bin/activate
   ```

   Example for Windows:

   ```bash
   .\venv\Scripts\activate
   ```

1. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd bindings/cedarling_python
   ```

1. **Install `maturin` dependencies** (optional, does not work for **windows**)::

   ```bash
   pip install maturin[patchelf]
   ```

1. **Build and install the package**:
   Build the Rust crate and install it into the virtual environment:

   ```bash
   maturin develop --release
   ```

1. **Verify installation**:
   Check that the library is installed by listing the installed Python packages:

   ```bash
   pip list
   ```

   You should see `cedarling_python` listed among the available packages.

   > If you want to install the package globally (not in a virtual environment), you can skip steps 2 and 3.

1. Read documentation
  After installing the package you can read the documentation from python using the following command:

   ```bash
   python -m pydoc cedarling_python
   ```

## Running a Python Script

To verify that the library works correctly, you can run the provided `example.py` script. Make sure the virtual environment is activated before running the script:

```bash
python example.py
```

## Building the Python Library

If you only want to build the library without installing it in the Python environment, follow these steps:

1. **Activate the virtual environment**:
   (Refer to steps 2 and 3 above if needed.)

1. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd bindings/cedarling_python
   ```

1. **Install `maturin` dependencies**:

   ```bash
   pip install maturin
   ```

   and (optional, for **windows** not work)

   ```bash
   pip install maturin[patchelf]
   ```

1. **Build the crate**:
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
  
  1. Make sure that you have installed the `cedarling_python` package in your virtual enviroment or system.
  1. Install `pytest`:

     ```bash
     pip install pytest
     ```

  1. Make sure that you are in the `bindings/cedarling_python/` folder.
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

  1. Make sure that you are in the `bindings/cedarling_python/` folder.
  1. Run the following command:

     ```bash
     tox
     ```

  1. See the results in the terminal.
