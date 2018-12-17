# @project.version@ is set by maven plugin. See 'https://dmp.fabric8.io/#docker:build' filtering.

FROM docker.hawkore.com/hk/oraclejdk:8u151

MAINTAINER hawkore

ENV VERSION=@project.version@
ENV ARTIFACT=apache-ignite-fabric-lgpl-hk-$VERSION-bin.zip

# install ps, required by startup script
RUN DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -yq --no-install-recommends procps unzip

# Install ignite
COPY maven/$ARTIFACT /opt/$ARTIFACT
RUN unzip /opt/$ARTIFACT -d /opt/ && rm /opt/$ARTIFACT
RUN mv /opt/apache-ignite-fabric-lgpl-hk-$VERSION /opt/ignite
ENV IGNITE_HOME /opt/ignite

# Cleanup
RUN DEBIAN_FRONTEND=noninteractive apt-get purge -yq unzip && apt-get clean
RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Define mount points.
VOLUME ["/opt/ignite/config","/opt/ignite/bin","/opt/ignite/work"]

# Define working directory.
WORKDIR /opt/ignite

# Startup command
CMD $IGNITE_HOME/bin/ignite.sh
