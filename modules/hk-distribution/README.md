# Building Apache Ignite Fabric lgpl distribution with HK extensions

#### Build Apache Ignite + HK extensions distribution by your own

Build a modified version of Apache Ignite distribution including Hawkore's extensions.

**Requirements**

-  Java >= 1.8.0_65 (OpenJDK and Sun have been tested)
-  Maven >= 3.0 for build 

**Install on local maven repository and docker (optional)**

-  Clone Apache Ignite modified project: `git clone http://github.com/hawkore/ignite-hk.git`
-  Change to directory: `cd ignite-hk`
-  Checkout current version: `git checkout 2.7.0-hk`
-  Build stand-alone distribution
```
mvn  clean install -Dignite.edition=apache-ignite-lgpl -Prelease,all-java,all-scala,licenses,lgpl,ignored-test,tensorflow -DskipTests -Dmaven.javadoc.skip=true
``` 
-  Build stand-alone distribution + hk modules:
``` bash
mvn clean install -U -Pgenerate-assembly-maven,attach-assembly-maven -f modules/hk-distribution
```
-  (Optional) Build stand-alone distribution + hk modules + docker image deployment (requires docker running on local machine):
``` bash
mvn clean install -U -Pgenerate-assembly-maven,generate-assembly-docker,attach-assembly-maven,attach-assembly-docker -f modules/hk-distribution
```

**NOTE**: See [Apache Ignite docker image deployment at README.md](src/main/docker/README.md) to change default ignite cluster configuration files.



