import requests
import warnings

warnings.filterwarnings("ignore")

idp_host = 'ubuntu.lxd'
client_id = "2000.728d2500-5268-40a5-af61-4046ccc01c1a"
client_secret= "3XZXq5Z83OJd"

print("Performing STEP 1")
url = 'https://{}/jans-auth/restv1/device_authorization'.format(idp_host)
auth
step1 = requests.post(
            url=url,
            auth=(client_id, client_secret),
            data={'client_id': client_id, 'scope': 'openid+profile+email+offline_access'},
            verify=False
        )
print("After visiting the following url press enter")
step1_data = step1.json()
print(step1_data['verification_uri_complete'])
input("Press Enter after device was verified:")


print("Performing STEP 2")
step2 = requests.post(
            url='https://{}/jans-auth/restv1/token'.format(idp_host),
            auth=(client_id, client_secret),
            data=[
                ('client_id',client_id),
                ('scope','openid+profile+email+offline_access'),
                ('grant_type', 'urn:ietf:params:oauth:grant-type:device_code'),
                ('grant_type', 'refresh_token'),
                ('device_code',step1_data['device_code'])
                ],
             verify=False
            )
step2_data = step2.json()

print("Performing STEP 3")
step3 = requests.post(
            url='https://{}/jans-auth/restv1/userinfo'.format(idp_host),
            headers={'Authorization': 'Bearer {}'.format(step2_data['access_token'])},
            data={'access_token': step2_data['access_token']},
            verify=False
            )
step3_data = step3.text


print("Performing STEP 4")
step4 = requests.post(
            url='https://{}/jans-auth/restv1/token'.format(idp_host),
            headers={'Authorization': 'Bearer {}'.format(step2_data['access_token'])},
            data={'grant_type': 'client_credentials', 'scope': 'openid', 'ujwt': step3_data},
            verify=False
            )
step4_data = step4.json()

print(step4_data["access_token"])



"""
Equivalent curl commands"

# STEP 1
curl -k -X POST https://ubuntu.lxd/jans-auth/restv1/device_authorization -u "2000.728d2500-5268-40a5-af61-4046ccc01c1a:3XZXq5Z83OJd" -d "client_id=2000.728d2500-5268-40a5-af61-4046ccc01c1a&scope=openid%2Bprofile%2Bemail%2Boffline_access"

### STEP 2
curl -k -X POST https://ubuntu.lxd/jans-auth/restv1/token -u "2000.728d2500-5268-40a5-af61-4046ccc01c1a:3XZXq5Z83OJd" -d "client_id=2000.728d2500-5268-40a5-af61-4046ccc01c1a&scope=openid%2Bprofile%2Bemail%2Boffline_access&grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&grant_type=refresh_token&device_code=fb4920356e2f35de3c59e1d7a979902d197599ae373671e9"

### STEP 3
curl -k -X POST https://ubuntu.lxd/jans-auth/restv1/userinfo -H "Authorization: Bearer eyJraWQiOiJjb25uZWN0X2RmYzEyYjZlLTM5YzktNDYxYS04ZDRmLTY0ZTE3Y2E4YjFhOV9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInN1YiI6InZsd0ZZaUR5ekZvQlRCMlI2N01jSG1WQW5adkx4YVduR0txM1g2UlhieEEiLCJ4NXQjUzI1NiI6IiIsImNvZGUiOiI4ZTgyMGRlNS1hMTFlLTQ4NDEtOGI4Mi1iZDcyNmVmODIzMjgiLCJzY29wZSI6WyJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3N0YXRzLnJlYWRvbmx5IiwiamFuc19zdGF0IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2phbnMtYXV0aC1zZXJ2ZXIvY29uZmlnL2FkbWludWkvbGljZW5zZS5yZWFkb25seSIsImh0dHBzOi8vamFucy5pby9vYXV0aC9qYW5zLWF1dGgtc2VydmVyL2NvbmZpZy9hZG1pbnVpL2xpY2Vuc2Uud3JpdGUiLCJvcGVuaWQiXSwiaXNzIjoiaHR0cHM6Ly91YnVudHUubHhkIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTY5MTU3NzU1MywiaWF0IjoxNjkxNDExODcxLCJjbGllbnRfaWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInVzZXJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIn0.ZBHRqZyaTUqh4orp4FgTX33TfSfzCfCVnlCo-pjzLZM2m8SSJ1iqwCzbCLK-Jgt5_FWQ61P-n8CHcqExMjQYc5kYJQQX8ePEwSreaaT-cu6Hra11Gz8QD-XOFyUrjOzRlyL-4LvyZvQhwZvSqBU8sim5LFsVcCE8GF_qu9IdFHkTgaU8FuqacD6rFEvKukxoWWj7TpiC-kZ4t04TwAPmgwiM7oroPnFPajB3m-aClukldx9raPp0yq6gsXAATFecVldsJyUffX0PC5ToqhRTR6j5e6dqht0NSVYxJQ3UoDYwLcWqIfbzjIr75GIl_25Yi9qGjfxbVxvgJWJ1OPzN9Q" -d "access_token=eyJraWQiOiJjb25uZWN0X2RmYzEyYjZlLTM5YzktNDYxYS04ZDRmLTY0ZTE3Y2E4YjFhOV9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInN1YiI6InZsd0ZZaUR5ekZvQlRCMlI2N01jSG1WQW5adkx4YVduR0txM1g2UlhieEEiLCJ4NXQjUzI1NiI6IiIsImNvZGUiOiI4ZTgyMGRlNS1hMTFlLTQ4NDEtOGI4Mi1iZDcyNmVmODIzMjgiLCJzY29wZSI6WyJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3N0YXRzLnJlYWRvbmx5IiwiamFuc19zdGF0IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2phbnMtYXV0aC1zZXJ2ZXIvY29uZmlnL2FkbWludWkvbGljZW5zZS5yZWFkb25seSIsImh0dHBzOi8vamFucy5pby9vYXV0aC9qYW5zLWF1dGgtc2VydmVyL2NvbmZpZy9hZG1pbnVpL2xpY2Vuc2Uud3JpdGUiLCJvcGVuaWQiXSwiaXNzIjoiaHR0cHM6Ly91YnVudHUubHhkIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTY5MTU3NzU1MywiaWF0IjoxNjkxNDExODcxLCJjbGllbnRfaWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInVzZXJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIn0.ZBHRqZyaTUqh4orp4FgTX33TfSfzCfCVnlCo-pjzLZM2m8SSJ1iqwCzbCLK-Jgt5_FWQ61P-n8CHcqExMjQYc5kYJQQX8ePEwSreaaT-cu6Hra11Gz8QD-XOFyUrjOzRlyL-4LvyZvQhwZvSqBU8sim5LFsVcCE8GF_qu9IdFHkTgaU8FuqacD6rFEvKukxoWWj7TpiC-kZ4t04TwAPmgwiM7oroPnFPajB3m-aClukldx9raPp0yq6gsXAATFecVldsJyUffX0PC5ToqhRTR6j5e6dqht0NSVYxJQ3UoDYwLcWqIfbzjIr75GIl_25Yi9qGjfxbVxvgJWJ1OPzN9Q"

### STEP 4
curl -k -X POST https://ubuntu.lxd/jans-auth/restv1/token -H "Authorization: Bearer eyJraWQiOiJjb25uZWN0X2RmYzEyYjZlLTM5YzktNDYxYS04ZDRmLTY0ZTE3Y2E4YjFhOV9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInN1YiI6InZsd0ZZaUR5ekZvQlRCMlI2N01jSG1WQW5adkx4YVduR0txM1g2UlhieEEiLCJ4NXQjUzI1NiI6IiIsImNvZGUiOiI4ZTgyMGRlNS1hMTFlLTQ4NDEtOGI4Mi1iZDcyNmVmODIzMjgiLCJzY29wZSI6WyJodHRwczovL2phbnMuaW8vb2F1dGgvY29uZmlnL3N0YXRzLnJlYWRvbmx5IiwiamFuc19zdGF0IiwiaHR0cHM6Ly9qYW5zLmlvL29hdXRoL2phbnMtYXV0aC1zZXJ2ZXIvY29uZmlnL2FkbWludWkvbGljZW5zZS5yZWFkb25seSIsImh0dHBzOi8vamFucy5pby9vYXV0aC9qYW5zLWF1dGgtc2VydmVyL2NvbmZpZy9hZG1pbnVpL2xpY2Vuc2Uud3JpdGUiLCJvcGVuaWQiXSwiaXNzIjoiaHR0cHM6Ly91YnVudHUubHhkIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTY5MTU3NzU1MywiaWF0IjoxNjkxNDExODcxLCJjbGllbnRfaWQiOiIyMDAwLjcyOGQyNTAwLTUyNjgtNDBhNS1hZjYxLTQwNDZjY2MwMWMxYSIsInVzZXJuYW1lIjoiRGVmYXVsdCBBZG1pbiBVc2VyIn0.ZBHRqZyaTUqh4orp4FgTX33TfSfzCfCVnlCo-pjzLZM2m8SSJ1iqwCzbCLK-Jgt5_FWQ61P-n8CHcqExMjQYc5kYJQQX8ePEwSreaaT-cu6Hra11Gz8QD-XOFyUrjOzRlyL-4LvyZvQhwZvSqBU8sim5LFsVcCE8GF_qu9IdFHkTgaU8FuqacD6rFEvKukxoWWj7TpiC-kZ4t04TwAPmgwiM7oroPnFPajB3m-aClukldx9raPp0yq6gsXAATFecVldsJyUffX0PC5ToqhRTR6j5e6dqht0NSVYxJQ3UoDYwLcWqIfbzjIr75GIl_25Yi9qGjfxbVxvgJWJ1OPzN9Q" -d "grant_type=client_credentials&scope=openid&ujwt=eyJraWQiOiJjb25uZWN0X2RmYzEyYjZlLTM5YzktNDYxYS04ZDRmLTY0ZTE3Y2E4YjFhOV9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ2bHdGWWlEeXpGb0JUQjJSNjdNY0htVkFuWnZMeGFXbkdLcTNYNlJYYnhBIiwiYXVkIjoiMjAwMC43MjhkMjUwMC01MjY4LTQwYTUtYWY2MS00MDQ2Y2NjMDFjMWEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsIm5pY2tuYW1lIjoiQWRtaW4iLCJpc3MiOiJodHRwczovL3VidW50dS5seGQiLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJtaWRkbGVfbmFtZSI6IkFkbWluIiwiaW51bSI6IjA2NDU4NTUzLTYwNjAtNGFiMC05YzQzLWE4ODQyYjg2ODUwNiIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoiYWRtaW5AdWJ1bnR1Lmx4ZCIsImphbnNBZG1pblVJUm9sZSI6WyJhcGktYWRtaW4iXX0.GC2o8zOUQbvtTklqEG6YhnWNC965pOmh9T4EztfiBRS0zIyqkHYQ5oQWPPPG87AwD7cXzLNl_2HH4HaZLfloVc90O4YqwZeeNlJn0CFlZ55f9Bnq3mUi0t36b-GGsksV7bMUN_rSthRNOHcxTTNrAkZIsbLKd91eXFbwWR6KSX9MUr8oZ8Gp_s_-DknnofBRJIavI1sXVLpv6uj-cUTRphGBiodLlJU1fDd_XueMYhcKjeL1fn6gSwPQcItPWGs6JBz7TdH_iJoE7ryiJ10SeWBMWWiH4HOSG2zJ9I7i5u1nGzywfN-HfnVE6MCFrBK4WeExsYUg3rdNI9HhHhTCCg"
"""
