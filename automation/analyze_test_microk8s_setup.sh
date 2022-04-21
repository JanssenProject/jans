#!/bin/bash
set -euo pipefail

collect_logs() {
  DATE=$(date +%F_%H-%M-%S)
  SERVICE=$1
  APP="janssen-$SERVICE"
  BUILD=""
  if [[ $SERVICE == "auth" ]];then
    APP="janssen-$SERVICE-server"
  fi
  POD_NAME=$(kubectl get pods --selector=app="$APP" --output=jsonpath={.items[0]..metadata.name} -n jans)
  IFS="=" read name BUILD <<< "$(kubectl exec "$POD_NAME" -n jans -- printenv | grep "CN_BUILD_DATE=")"
  echo "Found $POD_NAME built on $BUILD"
  t=0
  while true; do
    if [[ $SERVICE == "client-api" ]];then
      kubectl cp -n jans "$POD_NAME":opt/client-api/logs/ jans-"$SERVICE"-logs && zip -r jans-"$SERVICE"-logs-"$DATE".zip jans-"$SERVICE"-logs && rm -rf jans-"$SERVICE"-logs/ && t=120 || t=$(( t + 60 ))
    else
      kubectl cp -n jans "$POD_NAME":opt/jans/jetty/jans-"$SERVICE"/logs jans-"$SERVICE"-logs && zip -r jans-"$SERVICE"-logs-"$DATE".zip jans-"$SERVICE"-logs && rm -rf jans-"$SERVICE"-logs/ && t=120 || t=$(( t + 60 ))
    fi
    if [[ $t == 120 ]];then
      break
    else
      echo "Pod is probably not up! Retrying fetching logs..."
      sleep 60
    fi
  done

  curl "https://chat.gluu.org/api/v1/rooms.upload/YNz6rg7eNpngiygkv" \
      -F "file=@jans-$SERVICE-logs-$DATE.zip" \
      -F "msg=$APP built on $BUILD ran into an issue. Here are the logs." \
      -F "description=These logs were autogenerated from an ephemeral environment" \
      -H "X-Auth-Token: $2" \
      -H "X-User-Id: $3"
}

kubectl get po -n jans
echo "--------------------------------------Outputting persistence loader logs--------------------------------------"
kubectl logs -l APP_NAME=persistence-loader -c persistence -n jans
echo "--------------------------------------------------------------------------------------------------------------"
echo "--------------------------------------Outputting configurator logs--------------------------------------------"
kubectl logs -l APP_NAME=configurator -c config -n jans
echo "--------------------------------------------------------------------------------------------------------------"

SERVICES="auth-server client-api fido2 scim"
for SERVICE in $SERVICES; do
  kubectl -n jans wait --for=condition=available --timeout=30s deploy/janssen-"$SERVICE" || collect_logs "${SERVICE%-server}" "$1" "$2"
done
