# Docker image with Hawkore's Ignite extensions

**hk/ignite-hk**

Based on hk/oraclejdk

Versions so far:

| Version          | Other tags | From               |
| ---------------- | ---------- | ------------------ |
| $VERSION         |            | hk/oraclejdk:8u151 |

Commands to build last version:


``` sh
$ echo Note that this will not work 'as is' because Dockerfile is parametrized.
$ docker build --force-rm --rm=true --squash -t docker.hawkore.com/hk/ignite-hk:$VERSION .
```

**Important**: build is done through maven command. For the curious:

``` sh
mvn install -Phawkore,generate-assembly-maven,generate-assembly-docker,attach-assembly-maven,attach-assembly-docker
```

# How to run a container
 - Manually. 

``` sh
$ docker run --name ignite-hk --rm -t -i hk/ignite-hk:$VERSION /bin/bash
```

 - Background

``` sh
docker run --name ignite-hk --rm hk/ignite-hk:$VERSION
```

# Overwrite default configuration:

HK Ignite docker image has some mount points that you could mount on external volumes when create docker container, this easy change default configuration:

* `/opt/ignite/config`: ignite configuration files.
* `/opt/ignite/bin`: ignite launcher scripts
* `/opt/ignite/work` : internal ignite work directory (data storage)
