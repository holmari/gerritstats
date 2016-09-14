#!/bin/sh
#
# Runs GerritStats, generating HTML output by default.
#

script_path="$(cd "$(dirname -- "$0")" || exit 1; pwd -P)"

out_path="$script_path/out-html"

cd "$script_path/GerritStats" || exit 1
rm -rf src/main/frontend/data && \
    java -Xmx4096m -Xms256m -jar build/libs/GerritStats.jar -o src/main/frontend/data "$@" || exit 1

npm run webpack && \
    cp -r "$script_path/GerritStats/src/main/frontend/data" "$out_path"

echo "Output generated to out-html."
