#!/bin/sh
java -Xmx4096m -Xms256m -jar GerritStats/build/libs/GerritStats.jar "$@"
