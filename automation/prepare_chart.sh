#!/bin/bash
set -e

mkdir -p /home/runner/work/test
git clone https://github.com/GluuFederation/cloud-native-edition.git /home/runner/work/test/
temp_chart_folder="/home/runner/work/test/pygluu/kubernetes/templates/helm/gluu/charts"

services="casa cr-rotate jackrabbit oxpassport oxshibboleth oxtrust radius"
for service in $services; do
  rm -rf $temp_chart_folder/$service
done

remove_all() {
  sed '/{{ if eq .Values.global.cnJackrabbitCluster/,/{{- end }}/d' \
  | sed '/{{- if eq .Values.global.cnJackrabbitCluster/,/{{- end }}/d' \
  | sed '/{{- if .Values.global.oxshibboleth.enabled }}/,/{{- end }}/d' \
  | sed '/cnJackrabbitCluster/d' \
  | sed '/JACKRABBIT/d' \
  | sed '/Casa/d' \
  | sed '/Passport/d' \
  | sed '/Radius/d' \
  | sed '/Oxtrust/d' \
  | sed '/Shib/d' \
  | sed '/oxshibboleth/d'
}

cat $temp_chart_folder/auth-server/templates/deployment.yml | remove_all > tmpfile && mv tmpfile \
$temp_chart_folder/auth-server/templates/deployment.yml

cat $temp_chart_folder/config/templates/configmaps.yaml | remove_all > tmpfile && mv tmpfile \
$temp_chart_folder/config/templates/configmaps.yaml

cat $temp_chart_folder/config/values.yaml | remove_all > tmpfile && mv tmpfile \
$temp_chart_folder/config/values.yaml

