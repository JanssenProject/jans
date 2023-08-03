rem @echo off

cd .\\jans-bom
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-core
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-orm
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-fido2
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-link
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-auth-server
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-scim
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..

cd .\\jans-config-api
cmd /C mvn -Dmaven.test.skip=true -Ddependency-check.skip=true clean
cd ..
