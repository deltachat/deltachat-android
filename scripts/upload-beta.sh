#!/usr/bin/env bash

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "this script uploads test apks to download.delta.chat/android/beta, both flavours:"
    echo "- 🍋 gplay (overwrites gplay installs)"
    echo "- 🍉 dev (can be installed beside gplay)"
    echo ""
    echo "usage: ./scripts/upload-beta.sh <VERSION>"
    exit
fi
if [[ ${VERSION:0:1} == "v" ]]; then
    echo "VERSION must not begin with 'v' here."
    exit
fi

APKGPLAY="gplay/release/deltachat-gplay-release-$VERSION.apk"
APKDEV="build/outputs/apk/foss/debug/deltachat-foss-debug-$VERSION.apk"
ls -l $APKGPLAY
ls -l $APKDEV
read -p "upload these apks to download.delta.chat/android/beta? ENTER to continue, CTRL-C to abort."

# If you want to be able to upload, send your public SSH key to sysadmin@testrun.org
if ! rsync --progress -e "ssh " "$APKGPLAY" www-download-android@download.delta.chat:/beta/; then
    echo "upload of $APKGPLAY failed, aborting." >&2
    exit 1
fi
if ! rsync --progress -e "ssh " "$APKDEV" www-download-android@download.delta.chat:/beta/; then
    echo "upload of $APKDEV failed, aborting." >&2
    exit 1
fi

echo "upload done."
echo ""
echo "and now: here is Delta Chat $VERSION - choose your flavour and mind your backups:"
echo "- 🍋 https://download.delta.chat/android/beta/deltachat-gplay-release-$VERSION.apk (google play candidate, overwrites existing installs, should keep data)"
echo "- 🍉 https://download.delta.chat/android/beta/deltachat-foss-debug-$VERSION.apk (f-droid candidate, can be installed beside google play)"
echo ""
echo "what to test: PLEASE_FILL_OUT"


