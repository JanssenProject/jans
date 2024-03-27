server := jans.test
username := test
password := test

all:
	zip -r test.gama ./*
	sshpass -p "$(password)" scp test.gama $(username)@$(server):~/

clean:
	rm test.gama
