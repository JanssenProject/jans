# cedarling_python 🐍

This project uses `maturin` to create a Python library from Rust code. Follow the steps below to install and build the library.

### Prerequisites

1. **Install `maturin`:**
   We recommend using `pipx` to install `maturin`. First, ensure `pipx` is installed by following the [official instructions](https://pipx.pypa.io/stable/).

   To install `maturin` using `pipx`:

   ```bash
   pipx install maturin
   ```

2. **Ensure Rust is installed:**
   Verify Rust installation by running:
   ```bash
   cargo --version
   ```
   If Rust is not installed, you can install it from [here](https://www.rust-lang.org/tools/install).

---

## Installing the Python Library

Follow these steps to install the Python package in a virtual environment.

1. **Clone the repository**:
   Ensure the repository is loaded into your directory.

2. **Set up a virtual environment**:
   - Install `venv` for your platform by following the [instructions here](https://virtualenv.pypa.io/en/latest/installation.html).
   - Create a virtual environment:
     ```bash
     python3 -m venv venv
     ```
3. **Activate the virtual environment**:
   Follow the [instructions here](https://packaging.python.org/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment) to activate the virtual environment.

   Example for Linux/macOS:

   ```bash
   source venv/bin/activate
   ```

   Example for Windows:

   ```bash
   .\venv\Scripts\activate
   ```

4. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd cedarling_python
   ```

5. **Install `maturin` dependencies**:

   ```bash
   pip install maturin[patchelf]
   ```

6. **Build and install the package**:
   Build the Rust crate and install it into the virtual environment:

   ```bash
   maturin develop --release
   ```

7. **Verify installation**:
   Check that the library is installed by listing the installed Python packages:
   ```bash
   pip list
   ```

> If you want to install the package globally (not in a virtual environment), you can skip steps 2 and 3.

---

## Running a Python Script

To verify that the library works correctly, you can run the provided `example.py` script. Make sure the virtual environment is activated before running the script:

```bash
python example.py
```

---

## Only Building the Library

If you only want to build the library without installing it in the Python environment, follow these steps:

1. **Activate the virtual environment**:
   (Refer to steps 2 and 3 above if needed.)

2. **Navigate to the `cedarling_python` folder**:

   ```bash
   cd cedarling_python
   ```

3. **Install `maturin` dependencies**:

   ```bash
   pip install maturin[patchelf]
   ```

4. **Build the crate**:
   To build the library:
   ```bash
   maturin build --release
   ```
