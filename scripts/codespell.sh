#!/bin/sh
codespell \
  --skip './.git,./build,./res/values-*/strings.xml,,./assets/help,./jni/deltachat-core-rust' \
  --ignore-words-list formattings
