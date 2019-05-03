FROM elastisys/openjdk-public-dist:1.0
MAINTAINER Elastisys <techteam@elastisys.com>

# install bash shell in alpine linux
RUN apk add --no-cache bash

# install autoscaler server
COPY autoscaler.distro.standard.jar /opt/elastisys/autoscaler/autoscaler.server.jar
COPY start.sh /opt/elastisys/autoscaler/start.sh

ADD third-party-licenses /usr/share/doc/elastisys/third-party-licenses

# create configuration directory
COPY etc/elastisys/ /etc/elastisys/

# create directories
RUN mkdir -p /var/log/elastisys && \
    mkdir -p /var/run/elastisys/ && \
    mkdir -p /var/lib/elastisys/ && \
    chmod +x /opt/elastisys/autoscaler/start.sh

HEALTHCHECK --interval=10s --timeout=2s \
    CMD nc -z localhost ${HTTPS_PORT:-${HTTP_PORT}} || exit 1

CMD ["/opt/elastisys/autoscaler/start.sh"]
