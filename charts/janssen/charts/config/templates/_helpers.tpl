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
{{ $newList = append $newList ("redis" | quote )  }}
{{- end}}
{{ if eq .Values.global.cnPersistenceType "sql" }}
{{ $newList = append $newList ("sql" | quote) }}
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
