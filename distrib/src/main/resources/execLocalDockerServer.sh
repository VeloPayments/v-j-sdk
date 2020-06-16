#!/bin/bash
set -e

TAG=${1:-latest}
NETWORK=velo
$AGENTD_IMAGE="velopayments/velochain-agentd:$TAG"
$VAULT_IMAGE="jamesdbloom/mockserver:mockserver-:5.5.4"

if [[ $(curl -s -o /dev/null -w "%{http_code}" "https://index.docker.io/v1/") == "200" ]]
then
  docker pull $AGENTD_IMAGE
  docker pull $VAULT_IMAGE
else
  echo "Cannot reach docker.io - skipping docker pull."
fi

docker run -d  -p 4931:4931 --name agentd  $AGENTD_IMAGE
docker run -d  -p 1080:1080 --name vault  $VAULT_IMAGE -logLevel INFO -serverPort 1080

