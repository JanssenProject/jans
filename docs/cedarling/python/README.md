---
tags:
  - cedarling
  - python
---

# Python for Cedarling

Cedarling provides a binding for Python programs via the [pyo3](https://github.com/PyO3/pyo3) library. This allows Python developers to use the cedarling crate in their code directly. 

## Requirements

- Rust 1.63 or greater
- CPython 3.10 or greater
- For linux systems, installing `build-essential` using your distribution's package manager is recommended

## Building 

The recommended way to include cedarling in a Python project is to compile it to a python wheel via [Maturin](https://github.com/PyO3/maturin). Follow these steps to compile the wheel:

- Install `maturin` in an isolated environment. Using `venv` is recommended
  ```
  python -m venv venv
  source venv/bin/activate
  pip install maturin
  ```
  - For linux, install `maturin[patchelf]` instead to get the necessary dependencies
- Clone the [jans](https://github.com/JanssenProject/jans) repository and change your working directory to that location
- Navigate to `jans-cedarling/bindings/cedarling_python`
- Compile the binding:
  ```
  maturin build --release
  ```
- The wheel will be available in `target/wheels/`

- If you are developing a simple project in a `venv` setup, you can run `maturin develop --release` and maturin will install the wheel into the currently activated virtual environment. After that, you may run your code directly from the command line.

## Including in projects

In case of more complicated projects with a dependency manager such as [poetry](https://python-poetry.org/), you can either install the wheel via the command line:
```
poetry add path/to/wheel.whl
```
Or install it into the virtual environment managed by poetry:
```
poetry run pip install path/to/wheel.whl
```
or include it as a static dependency in the [dependencies](https://python-poetry.org/docs/pyproject/#dependencies-and-dependency-groups) section of your `pyproject.toml`:
```
...
[tool.poetry.dependencies]
cedarling_python = {path = "path/to/wheel.whl"}
```

For other dependency managers, please check their documentation on how to include static wheels in a project.

## Usage

Once the wheel is included in your project, you may use classes from the `cedarling_python` module in your code, which acts as a Python wrapper around cedarling's functions. See the [usage](./usage.md) document for details.
