We use `maturin` to create python library

1. To install `maturin` we need to install pipx [link](https://pipx.pypa.io/stable/)
2. Install `maturin`

```
pipx install maturin
```

3. ensure that you installed rust

```
cargo version
```

if rust is not installed you can install from [here](https://www.rust-lang.org/tools/install)

---

# Installing python library

0. load this repository to your directory

If you want to install package to virtual python enviroment you need to do next steps:

1. install venv to your platform [link](https://virtualenv.pypa.io/en/latest/installation.html)
2. create virtual enviroment [link](https://packaging.python.org/en/latest/guides/installing-using-pip-and-virtual-environments/#create-a-new-virtual-environment)

```
python3 -m venv venv
```

3. activate enviroment [link](https://packaging.python.org/en/latest/guides/installing-using-pip-and-virtual-environments/#activate-a-virtual-environment)
4. with activated enviroment move to the folder

```
cd authz_python
```

5. install dependency for maurin

```
pip install maturin[patchelf]
```

7. build the crate via `maturin` and install to the python virtual enviroment

```
maturin develop --release
```

8. ensure that the library is installed

```
pip list
```

if you want to install library to system globally you can avoid first 3 steps

## Run python script

You can run `example.py` to check that all is worked. Ensure that you have activated venv

```
python example.py
```

# Only building

To build the library for python

4. with activated enviroment move to the folder

```
cd authz_python
```

5. install dependency for maurin

```
pip install maturin[patchelf]
```

7. build the crate via `maturin` and install to the python virtual enviroment

```
maturin build` --release
```
