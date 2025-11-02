#! /usr/bin/env python3
import os
import datetime
import requests
import json

cache_dir = '/opt/jans/jetty/jans-config-api/cache'
cache_file = os.path.join(cache_dir, 'agama_lab_projects.json')

if not os.path.exists(cache_dir):
    os.mkdir(cache_dir)


def get_projects():


    try:
        response = requests.get('https://github.com/orgs/GluuFederation/repositories?q=agama-&per_page=200', headers={"Accept":"application/json"})
        result = response.json()
        downloads = []
        for repo in result['payload']['orgReposPageRoute']['repositories']:
            repo_name = repo["name"]
            response = requests.get(f'https://api.github.com/repos/GluuFederation/{repo_name}/releases/latest', headers={'Accept': 'application/json'})
            result = response.json()
            if response.ok:
                for asset in result['assets']:
                    if asset['name'].endswith('.gama'):
                        downloads.append({'repository-name': repo_name, 'description': repo['description'], 'download-link': asset['browser_download_url']})
            else:
                raise Exception(result.get('message'))

        with open(cache_file, 'w') as w:
            json.dump(downloads, w, indent=2)

    except Exception as e:
        try:
            cache_time = os.path.getmtime(cache_file)
            with open(cache_file) as f:
                projects = json.load(f)
            dt = datetime.datetime.fromtimestamp(cache_time).astimezone(datetime.timezone.utc)
            return {'result': True, 'projects': projects, 'error': str(e), 'info': f"Projects list was obtained at {dt}" }
        except Exception as e:
            return {'result': True, 'projects': [], 'error': str(e)}


if __name__ == '__main__':
    result = get_projects()
    print(json.dumps(result, indent=2))
