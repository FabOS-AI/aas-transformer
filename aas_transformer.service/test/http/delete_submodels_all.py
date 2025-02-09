import base64

import requests

def encode_id(id: str):
    return base64.b64encode(id.encode('utf-8')).decode('utf-8')

aas_repo_base_url = "http://localhost:8081"
sm_repo_base_url = "http://localhost:8081"

# Get submodels
response = requests.get(sm_repo_base_url + "/submodels")
submodels = response.json()["result"]

# Delete all submodels:
for submodel in submodels:
    response = requests.delete(sm_repo_base_url + "/submodels/" + encode_id(submodel["id"]))
    print("Deleted submodel with id: " + submodel["id"])

# Get shells
response = requests.get(aas_repo_base_url + "/shells")
shells = response.json()["result"]

for shell in shells:
    response = requests.get(aas_repo_base_url + "/shells/" + encode_id(shell["id"]) + "/submodel-refs")
    submodel_refs = response.json()["result"]

    for submodel_ref in submodel_refs:
        submodel_id = submodel_ref["keys"][0]["value"]
        response = requests.delete(aas_repo_base_url + "/shells/" + encode_id(shell["id"]) + "/submodel-refs/" + encode_id(submodel_id))
        print("Deleted submodel ref with id: " + submodel_id)
