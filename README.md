# velochain-sdk

### Required

- Java 11
- Maven 3.6.*, configure with the standard Velo Payments settings file at `~/.m2/settings.xml`
- Docker 19.*

## Build and Execute

```
$ mvn clean install
```

### Modules

* _sdk_  - SDK library (*velochain-sdk.jar*). Note, Maven will execute tests using vjblockchain for either Mac or Linux based on the OS running the build.
* _sdk-all-linux_ - "fat" jar with dependencies, including native dependencies for Linux (velochain-all-linux.jar)
* _sdk-all-osx_ - "fat" jar with dependencies, including native dependencies for OSX / MacOS (velochain-all-osx.jar)
* _examples_  - Code examples
   * certs
   * game
* _distrib_ - Distribution ZIP file (*velochain-sdk-distrib-bin.zip*).

## Install Distribution

```bash
$ unzip velochain-sdk-${version}-bin.zip
```
Distribution includes:
```
velochain-sdk-${version}
├── README.md
├── execLocalDockerServer.sh
├── velochain-all-${version}-linux.jar
├── velochain-all-${version}-osx.jar
└── examples
    ├── pom.xml
    └── src
        └── main
            └── java
                └── example
                    └── cert
                        └── *.java

```

## Run agentd from Docker
```shell script
docker run -d --name agentd -p 4931:4931  velopayments/velochain-agentd
```

With a volume mounted for data:
```shell script
docker run --name agentd -p 4931:4931 --mount "type=bind,src=$(pwd)/data,dst=/opt/velochain/data" --user "$(id -u):$(id -g)" velopayments/velochain-agentd
```

Stopping the agentd container:
```
docker stop agentd
```
Removing the stopped container (and it's volume)
```
docker rm -v agentd
```
