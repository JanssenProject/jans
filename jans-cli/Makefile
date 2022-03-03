.DEFAULT_GOAL := develop

develop:
	pip3 install -e .

install:
	pip3 install .

uninstall:
	pip3 uninstall jans-cli -y

zipapp:
	shiv --compressed -o config-cli.pyz -p '/usr/bin/env python3' -e cli.config_cli:main . --no-cache
