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

cd "$script_path" || exit 1
rm -rf GerritStats/out-html/data && \
	java -Xmx4096m -Xms256m -jar GerritStats/build/libs/GerritStats.jar \
    -o GerritStats/out-html/data "${new_args[@]}" || exit 1

cd "$script_path/GerritStats" || exit 1
npm run webpack && \
	mkdir -p "$output_dir/data" && \
    cp -r "$script_path/GerritStats/out-html/data" "$output_dir/" && \
    cp -r "$script_path/GerritStats/out-html/"* "$output_dir/"

echo "Output generated to $output_dir"
