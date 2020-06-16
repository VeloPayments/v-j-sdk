#!/bin/bash -e

case $1 in
    start)
        echo "Starting $VELOCHAIN_HOME/bin/agentd as $(whoami)"
        exec $VELOCHAIN_HOME/bin/agentd -I start
        ;;
esac

# or run the specified command
exec "$@"
