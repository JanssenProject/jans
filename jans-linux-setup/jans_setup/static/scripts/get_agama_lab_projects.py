#! /usr/bin/env python3
import os
import datetime
import tempfile
import requests
import json

cache_dir = '/opt/jans/jetty/jans-config-api/cache'
cache_file = os.path.join(cache_dir, 'agama_lab_projects.json')

os.makedirs(cache_dir, exist_ok=True)

class AgamaProjectFetchError(Exception):
    """Raised when fetching Agama project fails."""
    pass

def get_projects():

    try:
        response = requests.get('https://github.com/orgs/GluuFederation/repositories?q=agama-&per_page=200', headers={"Accept":"application/json"}, timeout=30)
        result = response.json()
        downloads = []
        for repo in result['payload']['orgReposPageRoute']['repositories']:
            repo_name = repo["name"]
            response = requests.get(f'https://api.github.com/repos/GluuFederation/{repo_name}/releases/latest', headers={'Accept': 'application/json'}, timeout=30)
            result = response.json()
            if response.ok:
                for asset in result['assets']:
                    if asset['name'].endswith('.gama'):
                        downloads.append({'repository-name': repo_name, 'description': repo['description'], 'download-link': asset['browser_download_url']})
            elif 'rate limit' in result.get('message'):
                raise AgamaProjectFetchError(result.get('message'))

        tmp_fd, tmp_path = tempfile.mkstemp(dir=cache_dir, suffix='.tmp', text=True)
        try:
            with os.fdopen(tmp_fd, 'w', encoding='utf-8') as w:
                json.dump(downloads, w, indent=2)
                w.flush()
                os.fsync(w.fileno())
            os.replace(tmp_path, cache_file)
        except:
            os.unlink(tmp_path)
            raise

        return {'result': True, 'projects': downloads, 'error': None, 'info': 'Projects list fetched live'}

    except Exception as e:
        try:
            cache_time = os.path.getmtime(cache_file)
            with open(cache_file) as f:
                projects = json.load(f)
            dt = datetime.datetime.fromtimestamp(cache_time).astimezone(datetime.timezone.utc)
            return {'result': True, 'projects': projects, 'error': str(e), 'info': f"Projects list was obtained at {dt}" }
        except Exception as e:
            return {'result': True, 'projects': [], 'error': str(e), 'info': None}


if __name__ == '__main__':
    result = get_projects()
    print(json.dumps(result, indent=2))
