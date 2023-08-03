rem @echo off

cd .\\jans-bom
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-core
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-orm
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-eleven
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-fido2
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-link
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-auth-server
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-scim
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..

cd .\\jans-config-api
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean compile install
cd ..
