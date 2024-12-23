#! /usr/bin/env python3

import requests
import json

def get_projects():

    try:
        response = requests.get('https://github.com/orgs/GluuFederation/repositories?q=agama-', headers={"Accept":"application/json"})
        result = response.json()
        downloads = []
        for repo in result["payload"]["repositories"]:
            repo_name = repo["name"]
            response = requests.get(f'https://api.github.com/repos/GluuFederation/{repo_name}/releases/latest', headers={'Accept': 'application/json'})
            if response.ok:
                result = response.json()
                for asset in result['assets']:
                    if asset['name'].endswith('.gama'):
                        downloads.append({'repository-name': repo_name, 'description': repo['description'], 'download-link': asset['browser_download_url']})
        return {'result': True, 'projects': downloads, 'error': None}
    except Exception as e:
        return {'result': True, 'projects': [], 'error': str(e)}

if __name__ == '__main__':
    result = get_projects()
    print(json.dumps(result, indent=2))
