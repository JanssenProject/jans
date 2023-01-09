# jans.pycloudlib

Utilities for Janssen cloud deployment. Used in Janssen docker images.

## Developer guide

### Run testcases

Testcase files are available under `tests` directory.
To run testcase suite, follow steps below:

1. Install [tox](https://tox.wiki/en/latest/) by running `pip install tox`
2. Run `tox` executable to run all testcase suites (note, to run a single testcase suite, run `tox -- tests/path/to/testfile` instead; see avaiable test files under `tests` directory)

### Check docstrings

The sourcecode of `jans.pycloudlib` are heavily documented internally using docstrings.
To check whether they are missing docstrings, run the following steps:

1. Install [pydocstyle](http://www.pydocstyle.org/en/stable/) by running `pip install pydocstyle[toml]`
2. Check docstrings by running `pydocstyle jans`
3. Adjust docstrings if any error is reported by `pydocstyle`

### Check Python types

We are adding more typehints into the `jans.pycloudlib` sourcecode, gradually.

1. Run `pip install -r requirements-dev.txt` to install required libraries.
2. Check typehints by running `mypy --install-types jans`
3. Fix errors reported by `mypy`

### Building internal docs

Internal docs are generated from mkdocs-based docs at `docs` directory.
To generate/preview docs, run the following steps:

1. Run `pip install -r requirements-dev.txt` to install required libraries.
2. Preview docs by running `mkdocs serve -w jans`
3. Visit http://localhost:8000 to see the generated docs (they are reloaded automatically when source code is modified)

## Refs

- https://www.linuxfoundation.org/press-release/2020/12/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation/
- https://betanews.com/2020/12/08/linux-foundation-open-source-identity-management/
- https://www.techrepublic.com/article/linux-foundation-debuts-new-secure-open-source-cloud-native-access-management-software-platform/

