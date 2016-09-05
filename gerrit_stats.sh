#!/bin/sh
java -Xmx2048m -Xms256m -jar GerritStats/build/libs/GerritStats.jar "$@"
