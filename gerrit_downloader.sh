#!/bin/sh

script_path="$(cd "$(dirname -- "$0")" || exit ; pwd -P)"

java -Xmx4096m -Xms256m -jar "$script_path/GerritDownloader/build/libs/GerritDownloader.jar" "$@"
