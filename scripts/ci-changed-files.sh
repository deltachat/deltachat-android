#!/usr/bin/env bash
# Prints "true" if any file changed between revisions matches the patterns
# read from stdin.
#
# Usage: ci-changed-files.sh <git-diff-revisions...> < patterns
#   CI:    ci-changed-files.sh HEAD^1 HEAD
#   Local: ci-changed-files.sh origin/main...HEAD
#
# Patterns work like GitHub on.<event>.paths: evaluated in order, a leading "!"
# excludes, the last match wins.
# Unlike GitHub, "*" also matches "/", so "src/main/*" covers whole subtree.
set -euo pipefail

if (($# == 0)); then
  echo "usage: $0 <git-diff-revisions...> < patterns" >&2
  exit 2
fi

patterns=()
while IFS= read -r p || [[ -n $p ]]; do
  [[ -z $p || $p == \#* ]] && continue
  patterns+=("$p")
done
if ((${#patterns[@]} == 0)); then
  echo "no patterns given on stdin" >&2
  exit 2
fi

# Use a file, so a git failure exits non-zero.
changed=$(mktemp)
trap 'rm -f "$changed"' EXIT
git diff --name-only --no-renames -z "$@" >"$changed"

while IFS= read -r -d '' f; do
  match=false
  for p in "${patterns[@]}"; do
    # The glob matchings are on purpose.
    if [[ $p == '!'* ]]; then
      # shellcheck disable=SC2053
      [[ $f == ${p:1} ]] && match=false
    else
      # shellcheck disable=SC2053
      [[ $f == $p ]] && match=true
    fi
  done
  if $match; then
    echo "Relevant change: $f" >&2
    echo true
    exit 0
  fi
done <"$changed"

echo "No relevant changes in $(tr -cd '\0' <"$changed" | wc -c) changed file(s)" >&2
echo false
