{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "cn.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "cn.fullname" -}}
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
{{- define "cn.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create configuration schema-related objects.
*/}}
{{- define "cn.config.schema" -}}
{{- $commonName := (printf "%s-configuration-file" .Release.Name) -}}
{{- $secretName := .Values.global.cnConfiguratorCustomSchema.secretName | default $commonName -}}
{{- $keyName := (printf "%s-configuration-key-file" .Release.Name) -}}
volumes:
  - name: {{ $commonName }}
    secret:
      secretName: {{ $secretName }}
{{- if .Values.global.cnConfiguratorKey }}
  - name: {{ $keyName }}
    secret:
      secretName: {{ $keyName }}
{{- end }}
volumeMounts:
  - name: {{ $commonName }}
    mountPath: {{ .Values.global.cnConfiguratorConfigurationFile }}
    subPath: {{ .Values.global.cnConfiguratorConfigurationFile | base }}
{{- if .Values.global.cnConfiguratorKey }}
  - name: {{ $keyName }}
    mountPath: {{ .Values.global.cnConfiguratorKeyFile }}
    subPath: {{ .Values.global.cnConfiguratorKeyFile | base }}
{{- end }}
{{- end }}
