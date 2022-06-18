# jans.pycloudlib

Utilities for Janssen cloud deployment. Used in Janssen docker images.

## Developer guide

### Run testcases

Testcase files are available under `tests` directory.
To run testcase suite, follow steps below:

1. Install [tox](https://tox.wiki/en/latest/) by running `pipx install tox`
1. Run `tox` executable to run all testcase suites (note, to run a single testcase suite, run `tox -- tests/path/to/testfile` instead; see avaiable test files under `tests` directory)

### Check docstrings

The sourcecode of `jans.pycloudlib` are heavily documented internally using docstrings.
To check whether they are missing docstrings, run the following steps:

1. Install [pydocstyle](http://www.pydocstyle.org/en/stable/) by running `pipx install pydocstyle[toml]`
1. Check docstrings by running `pydocstyle`
1. Adjust docstrings if any error is reported by `pydocstyle`

### Check Python types

We are adding more typehints into the `jans.pycloudlib` sourcecode, gradually.

1. Install [mypy](https://mypy.readthedocs.io/en/stable/index.html) by running `pipx install mypy`
1. Check typehints by running `mypy /path/to/python/file`
1. Fix errors reported by `mypy`

### Building internal docs

Internal docs are generated from sphinx-based docs at `docs` directory.
To generate/preview docs, run the following steps:

1. Install [sphinx-autobuild](https://github.com/executablebooks/sphinx-autobuild) by running `pipx install sphinx-autobuild`
1. Generate docs by running `sphinx-autobuild --watch=jans docs docs/_build/html`
1. Visit http://localhost:8000 to see the generated docs (they are reloaded automatically when sourcecode is modified)

## Refs

- https://www.linuxfoundation.org/press-release/2020/12/the-janssen-project-takes-on-worlds-most-demanding-digital-trust-challenges-at-linux-foundation/
- https://betanews.com/2020/12/08/linux-foundation-open-source-identity-management/
- https://www.techrepublic.com/article/linux-foundation-debuts-new-secure-open-source-cloud-native-access-management-software-platform/
