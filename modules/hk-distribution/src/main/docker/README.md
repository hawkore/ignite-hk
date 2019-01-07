# Docker image with Hawkore's Ignite extensions

**docker.hawkore.com/hk/ignite-hk**

Based on lwieske/java-8:jdk-8u151

Versions so far:

| image:version          | Other tags | From               |
| ---------------- | ---------- | ------------------ |
| **docker.hawkore.com/hk/ignite-hk:2.7.0-hk**           |            | lwieske/java-8:jdk-8u151 |

**Important:** Docker image is based on **lwieske/java-8:jdk-8u151**. Please note that docker image are for testing purposes only. You should manage your own **java-8 base docker image**.

**Important**: build is done through maven command. For the curious:

``` sh
mvn clean install -U -Pgenerate-assembly-maven,generate-assembly-docker,attach-assembly-maven,attach-assembly-docker -f modules/hk-distribution
```

# How to run a container
 - Manually. 

``` sh
$ docker run --name ignite-hk --rm -t -i docker.hawkore.com/hk/ignite-hk:2.7.0-hk /bin/bash
```

 - Background

``` sh
docker run --name ignite-hk --rm docker.hawkore.com/hk/ignite-hk:2.7.0-hk
```

# Overwrite default configuration:

HK Ignite docker image has some mount points that you could mount on external volumes when create docker container, this easy change default configuration:

* `/opt/ignite/config`: ignite configuration files.
* `/opt/ignite/bin`: ignite launcher scripts
* `/opt/ignite/work` : internal ignite work directory (data storage)
