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
{{ if or (eq .Values.global.cnPersistenceType "couchbase") (eq .Values.global.cnPersistenceType "hybrid") }}
{{ $newList = append $newList ("couchbase" | quote) }}
{{- end}}
{{ if eq .Values.global.cnPersistenceType "sql" }}
{{ $newList = append $newList ("sql" | quote) }}
{{- end }}
{{- if .Values.global.opendj.enabled}}
{{ $newList = append $newList ("ldap" | quote) }}
{{- end}}
{{- if .Values.global.fido2.enabled}}
{{ $newList = append $newList ("fido2" | quote) }}
{{- end}}
{{- if .Values.global.scim.enabled}}
{{ $newList = append $newList ("scim" | quote) }}
{{- end}}
{{ toJson $newList }}
{{- end }}