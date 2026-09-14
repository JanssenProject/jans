.PHONY: test clean all develop install uninstall

.DEFAULT_GOAL := develop

develop:
	pip install -r requirements-dev.txt

install:
	pip install .

uninstall:
	pip uninstall -y jans-pycloudlib

serve-docs:
	zensical serve

build-docs:
	zensical build
