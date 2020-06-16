#!/bin/bash -e

changeConfig() {
    cat << EOF > $1/etc/agentd.conf
listen 0.0.0.0:4931
canonization {
    max milliseconds 200
    max transactions 100
}
EOF
}

latestDistribution() {
    ls velochain-snapshot*.tar.xz | sort -n -t _ -k 2 | tail -1
}

dir=velochain
if [  -d "$dir" ] || [ -z "$(ls -A $dir)" ]; then
    rm -r $dir
fi
distrib=$(latestDistribution)
echo "Extract velochain distribution: $distrib"
tar -xvf $distrib

echo ""
echo "Replace ect/agentd.conf configuration"
changeConfig $dir


docker build --label build.time="$(date "+%Y-%m-%d% %H:%M:%S")" \
 --label velochain.agentd="$(shasum -a 256 $distrib)" \
 -t velopayments/velochain-agentd:latest .
