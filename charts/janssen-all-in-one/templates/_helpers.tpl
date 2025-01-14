{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "janssen-all-in-one.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "janssen-all-in-one.fullname" -}}
{{- if .Values.fullNameOverride -}}
{{- .Values.fullNameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "janssen-all-in-one.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
    Common labels
*/}}
{{- define "janssen-all-in-one.labels" -}}
app: {{ .Release.Name }}-{{ include "janssen-all-in-one.name" . }}-aio
helm.sh/chart: {{ include "janssen-all-in-one.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create user custom defined  envs
*/}}
{{- define "janssen-all-in-one.usr-envs"}}
{{- range $key, $val := .Values.usrEnvs.normal }}
- name: {{ $key }}
  value: {{ $val | quote }}
{{- end }}
{{- end }}

{{/*
Create user custom defined secret envs
*/}}
{{- define "janssen-all-in-one.usr-secret-envs"}}
{{- range $key, $val := .Values.usrEnvs.secret }}
- name: {{ $key }}
  valueFrom:
    secretKeyRef:
      name: {{ $.Release.Name }}-{{ $.Chart.Name }}-user-custom-envs
      key: {{ $key | quote }}
{{- end }}
{{- end }}

{{/*
Create optional scopes list
*/}}
{{- define "janssen-all-in-one.optionalScopes"}}
{{ $newList := list }}
{{- if eq .Values.configmap.cnCacheType "REDIS" }}
{{ $newList = append $newList "redis" }}
{{- end}}
{{ if eq .Values.cnPersistenceType "sql" }}
{{ $newList = append $newList "sql" }}
{{- end }}
{{ toJson $newList }}
{{- end }}

{{/*
Create topologySpreadConstraints lists
*/}}
{{- define "janssen-all-in-one.topology-spread-constraints"}}
{{- range $key, $val := .Values.topologySpreadConstraints }}
- maxSkew: {{ $val.maxSkew }}
  {{- if $val.minDomains }}
  minDomains: {{ $val.minDomains }} # optional; beta since v1.25
  {{- end}}
  {{- if $val.topologyKey }}
  topologyKey: {{ $val.topologyKey }}
  {{- end}}
  {{- if $val.whenUnsatisfiable }}
  whenUnsatisfiable: {{ $val.whenUnsatisfiable }}
  {{- end}}
  labelSelector:
    matchLabels:
      app: {{ $.Release.Name }}-{{ include "janssen-all-in-one.name" $ }}-aio
  {{- if $val.matchLabelKeys }}
  matchLabelKeys: {{ $val.matchLabelKeys }} # optional; alpha since v1.25
  {{- end}}
  {{- if $val.nodeAffinityPolicy }}
  nodeAffinityPolicy: {{ $val.nodeAffinityPolicy }} # optional; alpha since v1.25
  {{- end}}
  {{- if $val.nodeTaintsPolicy }}
  nodeTaintsPolicy: {{ $val.nodeTaintsPolicy }} # optional; alpha since v1.25
  {{- end}}
{{- end }}
{{- end }}

{{/*
Create aio enabled list
*/}}
{{- define "janssen-all-in-one.aioComponents"}}
{{ $newList := list }}
{{- if .Values.config.enabled }}
{{ $newList = append $newList ("configurator")  }}
{{- end}}
{{ if .Values.persistence.enabled }}
{{ $newList = append $newList ("persistence-loader") }}
{{- end}}
{{ if index .Values "auth-server" "enabled" }}
{{ $newList = append $newList ("jans-auth") }}
{{- end }}
{{- if index .Values "config-api" "enabled" }}
{{ $newList = append $newList ("jans-config-api") }}
{{- end}}
{{- if .Values.link.enabled}}
{{ $newList = append $newList ("jans-link") }}
{{- end}}
{{- if .Values.fido2.enabled}}
{{ $newList = append $newList ("jans-fido2") }}
{{- end}}
{{- if .Values.casa.enabled}}
{{ $newList = append $newList ("jans-casa") }}
{{- end}}
{{- if .Values.scim.enabled}}
{{ $newList = append $newList ("jans-scim") }}
{{- end}}
{{- if .Values.saml.enabled}}
{{ $newList = append $newList ("jans-saml") }}
{{- end}}
{{ toJson $newList }}
{{- end }}


{{/*
Create AWS shared credentials.
*/}}
{{- define "janssen-all-in-one.aws-shared-credentials" }}
{{- $profile := .Values.configmap.cnAwsProfile }}
{{- if not $profile }}
{{- $profile = "default" }}
{{- end }}
{{- printf "[%s]\naws_access_key_id = %s\naws_secret_access_key = %s\n" $profile .Values.configmap.cnAwsAccessKeyId .Values.configmap.cnAwsSecretAccessKey }}
{{- end }}

{{/*
Create AWS config.
*/}}
{{- define "janssen-all-in-one.aws-config" }}
{{- $profile := .Values.configmap.cnAwsProfile }}
{{- if not $profile }}
{{- $profile = "default" }}
{{- end }}
{{- if ne $profile "default" }}
{{- $profile = printf "profile %s" .Values.configmap.cnAwsProfile }}
{{- end }}
{{- printf "[%s]\nregion = %s\n" $profile .Values.configmap.cnAwsDefaultRegion }}
{{- end }}

{{/*
Create configuration schema-related objects.
*/}}
{{- define "janssen-all-in-one.config.schema" -}}
{{- $commonName := (printf "%s-configuration-file" .Release.Name) -}}
{{- $secretName := .Values.cnConfiguratorCustomSchema.secretName | default $commonName -}}
{{- $keyName := (printf "%s-configuration-key-file" .Release.Name) -}}
volumes:
  - name: {{ $commonName }}
    secret:
      secretName: {{ $secretName }}
{{- if .Values.cnConfiguratorKey }}
  - name: {{ $keyName }}
    secret:
      secretName: {{ $keyName }}
{{- end }}
volumeMounts:
  - name: {{ $commonName }}
    mountPath: {{ .Values.cnConfiguratorConfigurationFile }}
    subPath: {{ .Values.cnConfiguratorConfigurationFile | base }}
{{- if .Values.cnConfiguratorKey }}
  - name: {{ $keyName }}
    mountPath: {{ .Values.cnConfiguratorKeyFile }}
    subPath: {{ .Values.cnConfiguratorKeyFile | base }}
{{- end }}
{{- end }}

{{/*
Obfuscate configuration schema (only if configuration key is available)
*/}}
{{- define "janssen-all-in-one.config.prepareSchema" }}

{{- $configmapSchema := dict }}
{{- $_ := set $configmapSchema "hostname" .Values.fqdn }}
{{- $_ := set $configmapSchema "country_code" .Values.countryCode }}
{{- $_ := set $configmapSchema "state" .Values.state }}
{{- $_ := set $configmapSchema "city" .Values.city }}
{{- $_ := set $configmapSchema "admin_email" .Values.email }}
{{- $_ := set $configmapSchema "orgName" .Values.orgName }}
{{- $_ := set $configmapSchema "auth_sig_keys" (index .Values "auth-server" "authSigKeys") }}
{{- $_ := set $configmapSchema "auth_enc_keys" (index .Values "auth-server" "authEncKeys") }}
{{- $_ := set $configmapSchema "optional_scopes" (include "janssen-all-in-one.optionalScopes" . | trim) }}
{{- if .Values.saml.enabled }}
{{- $_ := set $configmapSchema "kc_admin_username" .Values.configmap.kcAdminUsername }}
{{- end }}
{{- $_ := set $configmapSchema "init_keys_exp" (index .Values "auth-server-key-rotation" "initKeysLife") }}

{{- $secretSchema := dict }}
{{- $_ := set $secretSchema "admin_password" .Values.adminPassword }}
{{- $_ := set $secretSchema "redis_password" .Values.redisPassword }}
{{- if or ( eq .Values.cnPersistenceType "sql" ) ( eq .Values.cnPersistenceType "hybrid" ) }}
{{- $_ := set $secretSchema "sql_password" .Values.configmap.cnSqldbUserPassword }}
{{- end }}
{{- if eq .Values.configSecretAdapter "vault" }}
{{- $_ := set $secretSchema "vault_role_id" .Values.configmap.cnVaultRoleId }}
{{- $_ := set $secretSchema "vault_secret_id" .Values.configmap.cnVaultSecretId }}
{{- end }}
{{- if or (eq .Values.configSecretAdapter "google") (eq .Values.configAdapterName "google") }}
{{- $_ := set $secretSchema "google_credentials" .Values.configmap.cnGoogleSecretManagerServiceAccount }}
{{- end }}
{{- if or (eq .Values.configAdapterName "aws") (eq .Values.configSecretAdapter "aws") }}
{{- $_ := set $secretSchema "aws_credentials" (include "config.aws-shared-credentials" . | b64enc) }}
{{- $_ := set $secretSchema "aws_config" (include "config.aws-config" . | b64enc) }}
{{- $_ := set $secretSchema "aws_replica_regions" (toJson .Values.configmap.cnAwsSecretsReplicaRegions | b64enc) }}
{{- end }}
{{- if .Values.saml.enabled }}
{{- $_ := set $secretSchema "kc_db_password" .Values.configmap.kcDbPassword }}
{{- $_ := set $secretSchema "kc_admin_password" .Values.configmap.kcAdminPassword }}
{{- end }}
{{- $_ := set $secretSchema "encoded_salt" .Values.salt }}

{{- $schema := dict "_configmap" $configmapSchema "_secret" $secretSchema }}

{{- if .Values.cnConfiguratorKey }}
{{- printf "%s" (encryptAES .Values.cnConfiguratorKey (toPrettyJson $schema)) }}
{{- else -}}
{{- toPrettyJson $schema }}
{{- end }}

{{/* end of helpers */}}
{{- end }}
