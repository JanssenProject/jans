%define name1 oxd-server
Name:           oxd-server
Version:        3.1.4
Release:        1.centos6
Summary:        plugins for OpenID and UMA
Group:          System Environment/Daemons
License:        MIT
URL:            http://www.gluu.org
Source0:        %{name}-%{version}.tar.gz
Source1:        oxd-server.init.d
Source2:        oxd-https-extension
Source3:        oxd-server-default
BuildArch:      noarch
Conflicts:      oxd-server

%description
oxd - OpenID Connect and UMA plugins for
Apache and nginx web containers

%prep
%setup -qn %{name}-%{version}

%build
#mvn clean package -U -Dmaven.test.skip=true


%install
rm -rf $RPM_BUILD_ROOT
install -d %{buildroot}/opt/%{name1}
install -d %{buildroot}/opt/%{name1}/conf
install -d %{buildroot}/opt/%{name1}/bin
install -d %{buildroot}/opt/%{name1}/lib
install -d %{buildroot}/etc/init.d
install -d %{buildroot}/%{_initddir}
install -d %{buildroot}/opt/oxd-https-extension/lib/
install -d %{buildroot}/etc/default/
install -m 755 %SOURCE1 %{buildroot}/etc/init.d/%{name1}
install -m 755 %SOURCE2 %{buildroot}/etc/init.d/oxd-https-extension
install -m 755 %SOURCE1 %{buildroot}/%{_initddir}/%{name1}
install -m 755 %SOURCE2 %{buildroot}/%{_initddir}/oxd-https-extension
install -m 644 %SOURCE3 %{buildroot}/etc/default/oxd-server
install -m 755 oxd-server/src/main/bin/oxd-start.sh %{buildroot}/opt/%{name1}/bin
install -m 755 oxd-server/src/main/bin/lsox.sh %{buildroot}/opt/%{name1}/bin
install -m 644 oxd-server/src/main/resources/log4j.xml %{buildroot}/opt/%{name1}/conf/
install -m 644 oxd-server/src/main/resources/oxd-conf.json %{buildroot}/opt/%{name1}/conf/
install -m 644 oxd-server/src/main/resources/oxd-default-site-config.json %{buildroot}/opt/%{name1}/conf/
install -m 644 oxd-server/target/oxd-server-jar-with-dependencies.jar %{buildroot}/opt/%{name1}/lib/
install -m 644 oxd-https-extension/target/oxd-https-extension-3.1.4-SNAPSHOT.jar %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 oxd-https-extension/oxd-https.keystore %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 oxd-https-extension/oxd-https.yml %{buildroot}/opt/oxd-https-extension/lib/
install -m 644 README.md %{buildroot}/opt/%{name1}/
install -m 644 LICENSE %{buildroot}/opt/%{name1}/
install -m 644 bcprov-jdk15on-1.54.jar %{buildroot}/opt/%{name1}/lib/
mkdir -p %{buildroot}/var/log/oxd-server
mkdir -p %{buildroot}/etc/oxd/oxd-server
mkdir -p %{buildroot}/etc/default/
cp -a %{buildroot}/opt/oxd-server/conf/* %{buildroot}/etc/oxd/oxd-server/.
cp -a debian/oxd-server-default %{buildroot}/etc/default/oxd-server

%clean
rm -rf $RPM_BUILD_ROOT

%post
chkconfig --add oxd-server
getent passwd jetty > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/useradd --system --create-home --user-group --shell /bin/bash --home-dir /home/jetty jetty
fi
getent group gluu > /dev/null 2>&1
if [ $? -ne 0 ]; then
    /usr/sbin/groupadd gluu
    /usr/sbin/usermod -a -G gluu jetty
fi

chown root:root /etc/default/oxd-server 2>&1
chown jetty:jetty -R /opt/oxd-server 2>&1
chmod 644 /etc/default/oxd-server 2>&1
mkdir -p /var/log/oxd-server 2>&1
chown jetty:jetty /var/log/oxd-server 2>&1

%preun
if [ -x “/etc/init.d/oxd-server” ] || [ -e “/etc/init/oxd-server.conf” ]; then
service oxd-server stop || exit $?
fi
chkconfig --del oxd-server

%files
%defattr(-,root,root,-)
/opt/%{name1}/*
/opt/oxd-https-extension/*
/etc/init.d/%{name1}
/etc/init.d/oxd-https-extension
%{_initddir}/%{name1}
%{_initddir}/oxd-https-extension
/etc/default/oxd-server
/etc/oxd/*
/var/log/oxd-server

%changelog
* Mon Mar 07 2016 Adrian Alves <adrian@gluu.org> - 3.1.4-1
- Release 3.1.4
