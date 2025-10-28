#! /usr/bin/env python3
import os
import datetime
import requests
import json

refresh_interval = 10 * 60
cache_dir = '/opt/jans/jetty/jans-config-api/cache'
cache_file = os.path.join(cache_dir, 'agama_lab_projects.json')

if not os.path.exists(cache_dir):
    os.mkdir(cache_dir)

def get_projects():

    try:
        if os.path.exists(cache_file):
            cache_time = os.path.getmtime(cache_file)
            if datetime.datetime.now().timestamp() < cache_time + refresh_interval:
                with open(cache_file) as f:
                    projects = json.load(f)
                dt = datetime.datetime.fromtimestamp(cache_time).astimezone(datetime.timezone.utc)
                return {'result': True, 'projects': projects, 'error': None, 'info': f"Projects list was obtained at {dt}" }
    except Exception as e:
        pass

    try:

        response = requests.get('https://github.com/orgs/GluuFederation/repositories?q=agama-&per_page=200', headers={"Accept":"application/json"})
        result = response.json()
        downloads = []
        for repo in result['payload']['orgReposPageRoute']['repositories']:
            repo_name = repo["name"]
            response = requests.get(f'https://api.github.com/repos/GluuFederation/{repo_name}/releases/latest', headers={'Accept': 'application/json'})
            if response.ok:
                result = response.json()
                for asset in result['assets']:
                    if asset['name'].endswith('.gama'):
                        downloads.append({'repository-name': repo_name, 'description': repo['description'], 'download-link': asset['browser_download_url']})

        with open(cache_file, 'w') as w:
            json.dump(downloads, w, indent=2)

        return {'result': True, 'projects': downloads, 'error': None}

    except Exception as e:
        return {'result': True, 'projects': [], 'error': str(e)}

if __name__ == '__main__':
    result = get_projects()
    print(json.dumps(result, indent=2))
