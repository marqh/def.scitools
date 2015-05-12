Name:		scitcf
Version:	2.0
Release:	7
Summary:	Scitools CF linked data registry

License:	apache
URL:		https://github.com/wmo-registers/tt-acv-deploy

BuildRoot:	%{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
Source0:	%{name}-%{version}.tar.gz

Requires:       java-1.7.0-openjdk
Requires:       nginx
Requires:       tomcat7


%description
SciTools configuration of ukgov-ld linked data registry

%prep
%setup -q


%install
rm -rf $RPM_BUILD_ROOT
install -D etc/sudoers.d/uklSudoers.conf $RPM_BUILD_ROOT/etc/sudoers.d/uklSudoers.conf
mkdir -p $RPM_BUILD_ROOT/opt/ldregistry/ui
mkdir -p $RPM_BUILD_ROOT/opt/ldregistry/config
install -D var/lib/tomcat7/webapps/ROOT.war $RPM_BUILD_ROOT/var/lib/tomcat7/webapps/ROOT.war

%pre
SERVICE='tomcat'#7
if ps ax | grep -v grep | grep $SERVICE > /dev/null
then
    service tomcat7 stop
fi
alternatives --set java /usr/lib/jvm/jre-1.7.0-openjdk.x86_64/bin/java
rm -rf /var/lib/tomcat7/webapps/ROOT
rm -rf /var/lib/tomcat7/webapps/ROOT.war
rm -rf /opt/ldregistry
rm -rf /var/opt/ldregistry/userstore/db.lck
rm -rf /var/opt/ldregistry/userstore/dbex.lck


declare -a arr=("/var/log/ldregistry" "/var/opt/ldregistry")

for sd in "${arr[@]}"
do
    if [[ ! -d $sd ]]; then
	mkdir -p $sd
	chown root:tomcat $sd
	chmod 775 $sd
    fi
done

%post
ln -s /opt/oauth/oauth.conf /opt/ldregistry/config/oauth.conf
service tomcat7 start

%clean
rm -rf $RPM_BUILD_ROOT



%files
/etc/sudoers.d/uklSudoers.conf
%defattr(775,root,tomcat,775)
/var/lib/tomcat7/webapps/ROOT.war
%dir /opt/ldregistry
%dir /opt/ldregistry/ui
%dir /opt/ldregistry/config


%changelog
* Fri Mar 27 2015 markh <markh@metarelate.net> - 0.1-1
- Initial version
