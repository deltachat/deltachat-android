TEXT=$1

if [ -z "$TEXT" ]; then
    echo "this script searches for the string key given as the first parameter."
    echo "search is done in this repo and in ../deltachat-ios and in ../deltachat-desktop."
    echo "usage: ./scripts/grep-string.sh <STRING-KEY>"
    exit
fi

echo "==================== ANDROID USAGE ===================="
grep --exclude={*.apk,*.a,*.o,*.so,strings.xml} --exclude-dir={.git,.gradle,obj,release,.idea,build,deltachat-core-rust} -ri $TEXT .

echo "==================== IOS USAGE ===================="
grep --exclude=*.strings --exclude-dir={.git,libraries,Pods,deltachat-ios.xcodeproj,deltachat-ios.xcworkspace} -ri $TEXT ../deltachat-ios/

echo "==================== DESKTOP USAGE ===================="
grep  --exclude-dir={.git,_locales} -ri $TEXT ../deltachat-desktop/

echo "==================== NODE USAGE ===================="
grep  --exclude-dir={.git} -ri $TEXT ../deltachat-node/
