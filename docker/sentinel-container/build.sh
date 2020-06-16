#!/bin/bash -e

sdk_version=${1:-1.0.0-SNAPSHOT}
dir=velochain
jars=$dir/etc/jars
entities=$dir/etc/entities
sdk_jar=$jars/

writeConfig() {
    cat << EOF > $1/etc/sentinels.properties
agent.host=agentd
agent.port=4931
agent.max.connections=4
vault.host=vault
vault.port=11001
search.host=search
search.port=9200
EOF
}

writeKeyConfig() {
    #FIXME for demo only
    cat << EOF > $entities/sentinels.keyconfig
{
  "entityId" : "6121dc1d-87cf-4edb-b9d7-bddcacd967ea",
  "entityName" : "sentinel",
  "contentType" : "UnprotectedOnly",
  "secretKey" : "TMGpPIAL7a4hLWPRcjs8wQNVFSZs66LIznHZfl/qGno=",
  "signingKey" : {
    "privateKeyBase64" : "6dsioX1+dYC5NltQOLePk/9CZiUYJ3sjrGGm0QQTI0J+O+ICNVu3iDBZNwm8WZVY/pYIlvY9Wwz1iGw3wlr/EA==",
    "publicKeyBase64" : "fjviAjVbt4gwWTcJvFmVWP6WCJb2PVsM9YhsN8Ja/xA="
  },
  "encryptionKey" : {
    "privateKeyBase64" : "j4RSNxOWjpDMHj4erOVYXqUqpE74zsaQm2GMrm4knaI=",
    "publicKeyBase64" : "901QBxppWBTlpjURBHrczhE8I9ngZjcxFqhBSE5/XSY="
  }
}
EOF
}

getSDK() {
    jar="../../sdk-all/target/velochain-sdk-all-${sdk_version}-linux.jar"
    echo "Copy $jar to $sdk_jar"
    if [ -e "$jar" ]; then
        cp $jar $sdk_jar
    else
        mvn -e -B -DgroupId=com.velopayments.blockchain -DartifactId=velochain-sdk-all -Dversion=$sdk_version -Dpackaging=jar -Dclassifier=linux -Ddest=$sdk_jar  dependency:get
    fi
}

if [ -d "$dir" ]; then
    rm -r $dir
fi
mkdir -p $entities
mkdir -p $jars

echo "Copy velochain sdk: $sdk_jar"
getSDK
echo ""
writeConfig $dir
writeKeyConfig

docker build --label velochain.sentinels=$sdk_version -t velopayments/velochain-sentinels:latest .
