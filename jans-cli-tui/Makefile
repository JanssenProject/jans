.DEFAULT_GOAL := develop

develop:
	pip3 install -e .

install:
	pip3 install .

uninstall:
	pip3 uninstall jans-cli-tui -y

zipapp:
	shiv --compressed -o config-cli-tui.pyz -p '/usr/bin/env python3' -e cli_tui.jans_cli_tui:run . --no-cache
