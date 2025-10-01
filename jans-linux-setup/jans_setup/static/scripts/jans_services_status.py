#!/usr/bin/python3

import os
import sys
import json
import requests

class Status:
    NOT_PRESENT = 'Not present'
    DOWN = 'Down'
    RUNNING = 'Running'

_JANS_LOCK_SERVICE_ = 'jans-lock'
_KEYCLOAK_ = 'keycloak'
JANS_JETTY_DIR = '/opt/jans/jetty'

HEALTH_ENDPOINTS = {
            'jans-auth': ['http://localhost:8081/jans-auth/sys/health-check', []],
            'jans-config-api': ['http://localhost:8074/jans-config-api/api/v1/health/live', []],
            'jans-casa': ['http://localhost:8080/jans-casa/health-check', []],
            'jans-fido2': ['http://localhost:8073/jans-fido2/sys/health-check', []],
            'jans-scim': ['http://localhost:8087/jans-scim/sys/health-check', []],
            'jans-link': ['http://localhost:9091/jans-link/sys/health-check', []],
        }

services_status = {}

def get_endpint_data(endpoint, status_code_only=False):
    try:
        response = requests.get(endpoint)
        if response.status_code == 200:
            if status_code_only:
                return {'status': 'up'}
            if response.text.lower() == 'ok':
                return {'status': 'ok'}
            return response.json()
    except Exception as _:
        pass
    return {'status': 'downn'}


def is_service_up(data):
    if data.get('status', '').lower() in ('running', 'up', 'ok'):
        return Status.RUNNING
    return Status.DOWN


def check_jans_service_health(endpoint, status_code_only=False):
    endpoint_data = get_endpint_data(endpoint[0], status_code_only)

    for subservice in endpoint[1]:
        services_status[subservice] = is_service_up(endpoint_data.get(subservice, {}))

    return is_service_up(endpoint_data)

def get_service_status():

    service_list = list(HEALTH_ENDPOINTS.keys())
    for jservice in HEALTH_ENDPOINTS:
        service_list += HEALTH_ENDPOINTS[jservice][1]
    service_list.append(_JANS_LOCK_SERVICE_)
    service_list.append(_KEYCLOAK_)

    check_services = [sys.argv[1]] if len(sys.argv) > 1 and sys.argv[1] in service_list else []

    if (check_services and _JANS_LOCK_SERVICE_ in check_services) or not check_services:
        if os.path.exists(os.path.join(JANS_JETTY_DIR, _JANS_LOCK_SERVICE_)):
            HEALTH_ENDPOINTS[_JANS_LOCK_SERVICE_] = ['http://localhost:8076/jans-lock/sys/health-check', []]
        elif os.path.exists(os.path.join(JANS_JETTY_DIR, 'jans-auth/custom/libs/jans-lock-service.jar')):
            HEALTH_ENDPOINTS['jans-auth'][1].append(_JANS_LOCK_SERVICE_)
        else:
            services_status[_JANS_LOCK_SERVICE_] = Status.NOT_PRESENT

    for jservice in HEALTH_ENDPOINTS:
        if check_services and jservice not in check_services:
            continue
        if os.path.exists(os.path.join(JANS_JETTY_DIR, jservice)):
            services_status[jservice] = check_jans_service_health(HEALTH_ENDPOINTS[jservice])
        else:
            services_status[jservice] = Status.NOT_PRESENT

    if (check_services and _KEYCLOAK_ in check_services) or not check_services:
        if os.path.exists('/opt/keycloak'):
            services_status[_KEYCLOAK_] = check_jans_service_health(['http://localhost:8083/kc/admin/master/console/', []], True)
        else:
             services_status[_KEYCLOAK_] = Status.NOT_PRESENT

if __name__ == '__main__':
    get_service_status()
    if '--as-table' in sys.argv:
        max_service_str_lenght = 0
        for service in services_status:
            if len(service) > max_service_str_lenght:
                max_service_str_lenght = len(service)
        header = 'Service'.ljust(max_service_str_lenght + 3) + 'Status     '
        print(header)
        print('-'*len(header))

        for service in sorted(services_status.keys()):
            print(service.ljust(max_service_str_lenght + 3) + services_status[service])

    else:
        print(json.dumps(services_status, indent=2))
