#!/usr/bin/env bash

set -e # stop on all errors

git submodule update --init --recursive
cd jni/deltachat-core-rust
OLD=`git branch --show-current`
if [ $# -eq 0 ]; then
    echo "updates deltachat-core-rust submodule to a tag or to last commit of a branch."
    echo "usage: ./scripts/update-core.sh BRANCH_OR_TAG"
    echo "current branch: $OLD"
    exit
fi
NEW=$1

git fetch
git checkout $NEW
TEST=`git branch --show-current`
if [ "$TEST" == "$NEW" ]; then
    git pull
fi

commitmsg=`git log -1 --pretty=%s`
cd ../..


git add jni/deltachat-core-rust
git commit -m "update deltachat-core-rust to '$commitmsg' of '$NEW'"
echo "old: $OLD, new: $NEW"
echo "changes are committed to local repo."
echo "use 'git push' to use them or 'git reset HEAD~1; git submodule update --recursive' to abort."
