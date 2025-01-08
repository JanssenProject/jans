{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "config.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "config.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
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
{{- define "config.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
    Common labels
*/}}
{{- define "config.labels" -}}
app: {{ .Release.Name }}-{{ include "config.name" . }}-init-load
helm.sh/chart: {{ include "config.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create user custom defined  envs
*/}}
{{- define "config.usr-envs"}}
{{- range $key, $val := .Values.usrEnvs.normal }}
- name: {{ $key }}
  value: {{ $val | quote }}
{{- end }}
{{- end }}

{{/*
Create user custom defined secret envs
*/}}
{{- define "config.usr-secret-envs"}}
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
{{- define "config.optionalScopes"}}
{{ $newList := list }}
{{- if eq .Values.configmap.cnCacheType "REDIS" }}
{{ $newList = append $newList "redis"  }}
{{- end}}
{{ if eq .Values.global.cnPersistenceType "sql" }}
{{ $newList = append $newList "sql" }}
{{- end }}
{{ toJson $newList }}
{{- end }}

{{/*
Create AWS shared credentials.
*/}}
{{- define "config.aws-shared-credentials" }}
{{- $profile := .Values.configmap.cnAwsProfile }}
{{- if not $profile }}
{{- $profile = "default" }}
{{- end }}
{{- printf "[%s]\naws_access_key_id = %s\naws_secret_access_key = %s\n" $profile .Values.configmap.cnAwsAccessKeyId .Values.configmap.cnAwsSecretAccessKey }}
{{- end }}

{{/*
Create AWS config.
*/}}
{{- define "config.aws-config" }}
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
Obfuscate configuration schema (only if configuration key is available)
*/}}
{{- define "config.prepareSchema" }}

{{- $configmapSchema := dict }}
{{- $_ := set $configmapSchema "hostname" .Values.global.fqdn }}
{{- $_ := set $configmapSchema "country_code" .Values.countryCode }}
{{- $_ := set $configmapSchema "state" .Values.state }}
{{- $_ := set $configmapSchema "city" .Values.city }}
{{- $_ := set $configmapSchema "admin_email" .Values.email }}
{{- $_ := set $configmapSchema "orgName" .Values.orgName }}
{{- $_ := set $configmapSchema "auth_sig_keys" (index .Values "global" "auth-server" "authSigKeys") }}
{{- $_ := set $configmapSchema "auth_enc_keys" (index .Values "global" "auth-server" "authEncKeys") }}
{{- $_ := set $configmapSchema "optional_scopes" (include "config.optionalScopes" . | trim) }}
{{- if .Values.global.saml.enabled }}
{{- $_ := set $configmapSchema "kc_admin_username" .Values.configmap.kcAdminUsername }}
{{- end }}
{{- $_ := set $configmapSchema "init_keys_exp" (index .Values "global" "auth-server-key-rotation" "initKeysLife") }}

{{- $secretSchema := dict }}
{{- $_ := set $secretSchema "admin_password" .Values.adminPassword }}
{{- $_ := set $secretSchema "redis_password" .Values.redisPassword }}
{{- if or ( eq .Values.global.cnPersistenceType "sql" ) ( eq .Values.global.cnPersistenceType "hybrid" ) }}
{{- $_ := set $secretSchema "sql_password" .Values.configmap.cnSqldbUserPassword }}
{{- end }}
{{- if eq .Values.global.configSecretAdapter "vault" }}
{{- $_ := set $secretSchema "vault_role_id" .Values.configmap.cnVaultRoleId }}
{{- $_ := set $secretSchema "vault_secret_id" .Values.configmap.cnVaultSecretId }}
{{- end }}
{{- if or (eq .Values.global.configSecretAdapter "google") (eq .Values.global.configAdapterName "google") }}
{{- $_ := set $secretSchema "google_credentials" .Values.configmap.cnGoogleSecretManagerServiceAccount }}
{{- end }}
{{- if or (eq .Values.global.configAdapterName "aws") (eq .Values.global.configSecretAdapter "aws") }}
{{- $_ := set $secretSchema "aws_credentials" (include "config.aws-shared-credentials" . | b64enc) }}
{{- $_ := set $secretSchema "aws_config" (include "config.aws-config" . | b64enc) }}
{{- $_ := set $secretSchema "aws_replica_regions" (toJson .Values.configmap.cnAwsSecretsReplicaRegions | b64enc) }}
{{- end }}
{{- if .Values.global.saml.enabled }}
{{- $_ := set $secretSchema "kc_db_password" .Values.configmap.kcDbPassword }}
{{- $_ := set $secretSchema "kc_admin_password" .Values.configmap.kcAdminPassword }}
{{- end }}
{{- $_ := set $secretSchema "encoded_salt" .Values.salt }}

{{- $schema := dict "_configmap" $configmapSchema "_secret" $secretSchema }}

{{- if .Values.global.cnConfiguratorKey }}
{{- printf "%s" (encryptAES .Values.global.cnConfiguratorKey (toPrettyJson $schema)) }}
{{- else -}}
{{- toPrettyJson $schema }}
{{- end }}

{{/* end of helpers */}}
{{- end }}
