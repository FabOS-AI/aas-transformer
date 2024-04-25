#!/bin/bash

json_files_folder="/aas-transformer-initializer/json"
transformer_host=$TRANSFORMER_HOST
transformer_port=$TRANSFORMER_PORT
transformer_path=$TRANSFORMER_PATH

transformer_base_url="http://${transformer_host}:${transformer_port}${transformer_path}"

until curl -m 5 -s --location --request GET $transformer_base_url > /dev/null; do
  echo "AAS Transformer not available @ ${transformer_base_url} -> sleeping"
  sleep 2
done

for file_path in $json_files_folder/*json;
do
        echo "Import "$file_path" ..."
        curl -X 'POST' "${transformer_base_url}/aas/transformer" -H 'Content-Type: application/json' -d @$file_path
done
