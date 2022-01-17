.DEFAULT_GOAL := develop

develop:
	/usr/bin/env python3 setup.py develop

install:
	pip3 install .

uninstall:
	pip3 uninstall pygluu-kubernetes -y

zipapp:
	shiv --compressed -o pygluu-kubernetes.pyz -p '/usr/bin/env python3' -e pygluu.kubernetes.create:main . --no-cache

guizipapp:
	shiv --compressed -o pygluu-kubernetes-gui.pyz -p '/usr/bin/env python3' -e pygluu.kubernetes.gui.server:run . --no-cache
