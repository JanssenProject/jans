.DEFAULT_GOAL := develop

develop:
	/usr/bin/env python3 setup.py develop

install:
	pip3 install .

uninstall:
	pip3 uninstall jans-cli -y

zipapp:
	shiv --compressed -o jans-cli.pyz -p '/usr/bin/env python3' -e cli.config_cli:main . --no-cache
