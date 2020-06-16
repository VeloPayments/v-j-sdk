FROM ubuntu

ENV VELOCHAIN_HOME=/velochain
ENV USER=veloagent

RUN echo -n "2000:2000      hard  nofile         1048576\n2000:2000      soft  nofile         1048576\n" >> /etc/security/limits.conf && \
    groupadd -g 2000 $USER && \
    adduser --disabled-password --gecos '' --uid 2000 --gid 2000 $USER
USER $USER

COPY --chown=$USER:$USER velochain/ $VELOCHAIN_HOME
COPY entrypoint.sh /

USER root

EXPOSE 4931
VOLUME ["$VELOCHAIN_HOME/data","$VELOCHAIN_HOME/var","$VELOCHAIN_HOME/etc"]

ENTRYPOINT ["/entrypoint.sh"]
CMD ["start"]
