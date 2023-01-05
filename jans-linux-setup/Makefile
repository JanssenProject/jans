.DEFAULT_GOAL := develop

develop:
	/usr/bin/env python3 setup.py develop

install:
	pip3 install .

uninstall:
	pip3 uninstall jans-setup -y

zipapp:
	shiv --compressed -o jans-linux-setup.pyz -p '/usr/bin/env python3' -e jans_setup.jans_setup:main . --no-cache
