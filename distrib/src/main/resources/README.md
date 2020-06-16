# VeloChain SDK

Velochain distribution includes:

* velochain-all-${version}-linux.jar  
    "fat" jar with dependencies, including native dependencies for Linux
    
* velochain-all-${version}-osx.jar       
    "fat" jar with dependencies, including native dependencies for OSX / MacOS

* examples   
    Java code examples

* execLocalDockerServer.sh    
    Script which launches the VeloChain service from a Docker image.

## Setup

The SDK is a Java library with some native code, so a jar for the targeted platform must be selected. At this time the supported platforms are Linux and Mac. Other operating systems can be supported through a virtual machine or Docker container. Add one velochain-all-* jars as a project dependency the targeted platform. An example showing how the platform can be dynamically selected using Maven profiles in `examples/pom.xml`. 

certificates
externalRef
game
guards
sentinels

## Examples

The SDK includes examples of using VeloChain APIs. They have been organized by concept, and build on each other. The examples can be built and executed with Maven using the provided pom.xml file. 

    $ cd examples
    $ mvn clean verify

To view and run the applications from your IDE rather than from the command line, import the `examples` folder as a Maven project.

Before running the examples you should set up 

### certificates

This is a good place to start. These examples demonstrate building and parsing data for Velochain using Certificate objects. 

- `SimpleCertificate`   
A demonstration of creating and storing blockchain certificates

- `EncryptedCertificate`  
Shows how to define encrypted fields and signed certificates.
 
- `ArtifactTransactions`   
Demonstrates the use of certificates to define artifact transactions


### external references

External References allow transactions to provide strong anchoring to content stored off of the immutable blockchain, in the VeloChain "vault".

There are two types of external references. The first type allows Velochain to reliably associate blockchain transactions with files that are too large for a blockchain certificate, such as document images.  The second type of external reference provides the ability to extend blockchain transactions with "mutable fields".  These mutable fields allow the blockchain to _forget_ or purge data such as personal information. 

- `PaymentExample`  
Demonstrates a blockchain transaction to create a payment, including an external reference to an image of a receipt document, and an external reference to mutable fields with personally identifying information.

- `FileEncryptionExample`  
Shows how to encrypt a file so that only specific subscribers can decrypt it. 

### guards

_tbd_

### sentinels

_tbd_

### game

_tbd_


## Running VeloChain service

    $ ./execLocalDockerServer.sh

Once the service is running open the Velochain Explorer in a web browser at [http://localhost:8080](http://localhost:8080)



#### Troubleshooting

`Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?`  

The Docker daemon is probably not running or not installed.

`Error response from daemon: pull access denied for velopayments/velochain, repository does not exist or may require 'docker login'`

Authentication to the Dockerhub registry is needed. Usually, that would just require running the [`docker login`] (https://docs.docker.com/engine/reference/commandline/login/) command first.



