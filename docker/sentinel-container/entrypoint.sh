#!/bin/bash -e

case $1 in
    start)
        echo "Starting sentinels as $(whoami)"
        ls -1 $VELOCHAIN_HOME/etc/jars $PLUGINS

        if [ -z ${CONFIG+x} ]; then
            echo "CONFIG var is unset";
        else
            echo -n "$CONFIG" > $VELOCHAIN_HOME/etc/sentinels.properties
        fi
        echo "Using config: $VELOCHAIN_HOME/etc/sentinels.properties"
        cat $VELOCHAIN_HOME/etc/sentinels.properties
        debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007"
        defaultlog="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"
        exec java -Xmx512m $debug $defaultlog -cp $VELOCHAIN_HOME/etc/jars/*:$PLUGINS/* com.velopayments.blockchain.sdk.SentinelContainer $VELOCHAIN_HOME/etc/sentinels.properties $VELOCHAIN_HOME/etc/entities/sentinels.keyconfig
        ;;
esac

# or run the specified command
exec "$@"
