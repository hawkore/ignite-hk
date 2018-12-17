# Building Apache Ignite Fabric lgpl distribution with HK extensions

## Apache Ignite Fabric lgpl distribution with HK extensions

from this source project (ignite-hk). Note replace `altDeploymentRepository` by yours:

``` sh
mvn install -U -Pgenerate-assembly-maven,generate-assembly-docker,attach-assembly-maven,attach-assembly-docker -f modules/hk-distribution
```

**NOTE**: See [src/main/docker/README.md](Apache Ignite docker image deployment at README.md) to change default ignite cluster configuration files.


## Build Apache Ignite with HK extensions by your own

### 1. Install apache ignite parent if not deployed yet (it's necessary in order to find apache-ignite zip distribution on repository)

from base ignite source project (ignite-hk). Note replace `altDeploymentRepository` by yours:

`mvn install -f parent/pom.xml` 

### 2. Install apache-ignite-fabric-lgpl

from base ignite source project (ignite-hk).

`mvn install -U -Prelease,all-java,all-scala,licenses,lgpl -Dignite.edition=fabric-lgpl`