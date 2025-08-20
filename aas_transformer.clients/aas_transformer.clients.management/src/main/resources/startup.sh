#!/usr/bin/env bash
input_file=swagger.json
output_file=swagger-tmp.json

# Download the swagger file from the management service
wget -O ${input_file} ${TRANSFORMER_MGMT_URL}/v3/api-docs

# Transform the swagger file to replace the schema for /message-events with a reference to the MessageEvent schema
jq '(.paths["/message-events"].get.responses."200".content["*/*"].schema.items) |= {"$ref": "#/components/schemas/MessageEvent"}' ${input_file} > ${output_file}

mv ${output_file} ${input_file}