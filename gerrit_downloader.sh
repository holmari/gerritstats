#!/bin/sh
java -Xmx4096m -Xms256m -jar GerritDownloader/build/libs/GerritDownloader.jar "$@"
