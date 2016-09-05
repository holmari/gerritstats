#!/bin/sh
java -Xmx2048m -Xms256m -jar GerritDownloader/build/libs/GerritDownloader.jar "$@"
