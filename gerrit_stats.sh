#!/bin/bash
#
# Runs GerritStats, generating HTML output by default.
#

script_path="$(cd "$(dirname -- "$0")" || exit 1; pwd -P)"
output_dir="$script_path/out-html"

new_args=()
index=0
next_is_output_dir=""

for arg in "$@"; do
    if [ "$next_is_output_dir" != "" ]; then
        output_dir="$arg"
        next_is_output_dir=""
    elif [ "$arg" == "-o" ]; then
        next_is_output_dir="1"
    else
        new_args[$index]="$arg"
        let "index += 1"
    fi
done

cd "$script_path/GerritStats" || exit 1
rm -rf src/main/frontend/data && \
	java -Xmx4096m -Xms256m -jar build/libs/GerritStats.jar -o src/main/frontend/data "${new_args[@]}" || exit 1

npm run webpack && \
	mkdir -p "$output_dir/data" && \
    cp -r "$script_path/GerritStats/src/main/frontend/data" "$output_dir/" && \
    cp -r "$script_path/GerritStats/out-html/"* "$output_dir/"

echo "Output generated to '$output_dir'."