FROM openjdk:11-jre-slim
#FROM velopayments/openjdk11-spring-runner:latest

ENV VELOCHAIN_HOME=/velochain
ENV PLUGINS=$VELOCHAIN_HOME/plugins

COPY velochain/ $VELOCHAIN_HOME/
COPY entrypoint.sh /entrypoint.sh
RUN mkdir -p $PLUGINS
VOLUME ["$PLUGINS"]

ENTRYPOINT ["/entrypoint.sh"]
CMD ["start"]
