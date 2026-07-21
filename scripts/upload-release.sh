#!/usr/bin/env bash

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "this script uploads release-ready apk and symbols to download.delta.chat/android"
    echo "- for showing up on get.delta.chat"
    echo "  you still need to change deltachat-pages/_includes/download-boxes.html"
    echo "- the script does not upload things to gplay or other stores."
    echo ""
    echo "usage: ./scripts/upload-release.sh <VERSION>"
    exit
fi
if [[ ${VERSION:0:1} == "v" ]]; then
    echo "VERSION must not begin with 'v' here."
    exit
fi

cd gplay/release
APK="deltachat-gplay-release-$VERSION.apk"
ls -l $APK
read -p "upload this apk and belonging symbols to download.delta.chat/android? ENTER to continue, CTRL-C to abort."

# If you want to be able to upload, send your public SSH key to sysadmin@testrun.org 
if ! rsync --progress $APK www-download-android@download.delta.chat:/ ; then
    echo "upload of $APK failed, aborting." >&2
    exit 1
fi


cd ../..
SYMBOLS_ZIP="$APK-symbols.zip"
rm $SYMBOLS_ZIP
zip -r $SYMBOLS_ZIP obj
zip $SYMBOLS_ZIP build/outputs/mapping/gplayRelease/mapping.txt
ls -l $SYMBOLS_ZIP
if ! rsync --progress $SYMBOLS_ZIP www-download-android@download.delta.chat:/symbols/ ; then
    echo "upload of $SYMBOLS_ZIP failed, aborting." >&2
    exit 1
fi


echo "upload done."
echo ""
echo "and now: here is Delta Chat $VERSION:"
echo "- 🍋 https://download.delta.chat/android/deltachat-gplay-release-$VERSION.apk (google play candidate, overwrites existing installs, should keep data)"
echo ""
