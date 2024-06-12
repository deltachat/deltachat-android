#!/bin/sh

MAIN=./src/main

codespell \
  --skip './.git,./build,$MAIN/res/values-*/strings.xml,,$MAIN/assets/help,./jni/deltachat-core-rust' \
  --ignore-words-list formattings
