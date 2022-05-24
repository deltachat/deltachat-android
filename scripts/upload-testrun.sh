
VERSION=$1

if [ -z "$VERSION" ]; then
    echo "this script uploads test apks testrun.org, both flavours:"
    echo "- 🍋 gplay (overwrites gplay installs)"
    echo "- 🍉 dev (can be installed beside gplay)"
    echo ""
    echo "usage: ./scripts/upload-testrun.sh <VERSION>"
    exit
fi
if [[ ${VERSION:0:1} == "v" ]]; then
    echo "VERSION must not begin with 'v' here."
    exit
fi

APKGPLAY="gplay/release/deltachat-gplay-release-$VERSION.apk"
APKDEV="build/outputs/apk/fat/debug/deltachat-fat-debug-$VERSION.apk"
ls -l $APKGPLAY
ls -l $APKDEV
read -p "upload these apks to mailadm.testrun.org? ENTER to continue, CTRL-C to abort."

scp $APKGPLAY root@mailadm.testrun.org:/var/www/testrun.org/
scp $APKDEV root@mailadm.testrun.org:/var/www/testrun.org/

echo "upload done."
echo ""
echo "and now: here is Delta Chat $VERSION - choose your flavour and mind your backups:"
echo "- 🍋 https://mailadm.testrun.org/deltachat-gplay-release-$VERSION.apk (android, overwrites existing gplay installs, should keep data)"
echo "- 🍉 https://mailadm.testrun.org/deltachat-fat-debug-$VERSION.apk (android, can be installed beside gplay)"
echo "- 🍏 https://testflight.apple.com/join/uEMc1NxS (ios, update to $VERSION may take a day or so)"


