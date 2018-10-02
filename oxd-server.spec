Name:           gluu-oxd-server
Version:        3.0.5
Release:        1%{?dist}
Summary:        plugins for OpenID and UMA
Group:          System Environment/Daemons
License:        MIT
URL:            http://www.gluu.org 
Source0:        %{name}-%{version}.tar.gz
Source1:	%{name}.init
BuildRequires:  apache-maven, java-1.7.0-openjdk-devel
Requires:       java-1.7.0-openjdk
BuildArch:      noarch

%description
oxd - OpenID Connect and UMA plugins for
Apache and nginx web containers

%prep
%setup -q


%build
mvn clean package -Dmaven.test.skip=true


%install
rm -rf $RPM_BUILD_ROOT
install -d %{buildroot}/opt/%{name}
install -d %{buildroot}/opt/%{name}/conf
install -d %{buildroot}/opt/%{name}/bin
install -d %{buildroot}/opt/%{name}/lib
install -m 755 oxd-server/src/main/bin/oxd-start.sh %{buildroot}/opt/%{name}/bin
install -m 755 oxd-server/src/main/bin/lsox.sh %{buildroot}/opt/%{name}/bin
install -m 644 oxd-server/src/main/resources/log4j.xml %{buildroot}/opt/%{name}/conf/
install -m 644 oxd-server/src/main/resources/oxd-default-site-config.json %{buildroot}/opt/%{name}/conf/
install -m 655 oxd-server/src/main/resources/oxd-conf.json %{buildroot}/opt/%{name}/conf/
install -m 644 oxd-server/target/oxd-server.jar %{buildroot}/opt/%{name}/lib/
install -m 644 oxd-client/target/oxd-client.jar %{buildroot}/opt/%{name}/lib/
install -m 644 oxd-common/target/oxd-common.jar %{buildroot}/opt/%{name}/lib/
install -m 644 README.md %{buildroot}/opt/%{name}/
install -m 644 LICENSE %{buildroot}/opt/%{name}/
install -d %{buildroot}/etc/init.d
install -m 755 %SOURCE1 %{buildroot}/etc/init.d/%{name}

%clean
rm -rf $RPM_BUILD_ROOT

%post
chkconfig --add %{name}
service %{name} start

%preun
service %{name} stop
chkconfig --del %{name}

%files
%defattr(-,root,root,-)
/opt/%{name}/*
/etc/init.d/%{name}

%changelog
* Mon Apr 20 2015 Adrian Alves <adrian@gluu.org> - 3.0.5-1
- Initial build
