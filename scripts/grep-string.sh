TEXT=$1

if [ -z "$TEXT" ]; then
    echo "search for a given string key in this repo"
    echo "and in ../deltachat-ios, ../deltachat-desktop, ../deltachat-core-rust/deltachat-jsonrpc, ../deltatouch"
    echo "usage: ./scripts/grep-string.sh <STRING-KEY>"
    exit
fi

echo "==================== ANDROID USAGE ===================="
grep --exclude={*.apk,*.a,*.o,*.so,strings.xml,*symbols.zip} --exclude-dir={.git,.gradle,obj,release,.idea,build,deltachat-core-rust} -ri $TEXT .

echo "==================== IOS USAGE ===================="
grep --exclude=*.strings* --exclude-dir={.git,libraries,Pods,deltachat-ios.xcodeproj,deltachat-ios.xcworkspace} -ri $TEXT ../deltachat-ios/

echo "==================== DESKTOP USAGE ===================="
grep --exclude-dir={.cache,.git,html-dist,node_modules,_locales} -ri $TEXT ../deltachat-desktop/

echo "==================== JSONRPC USAGE ===================="
grep  --exclude-dir={.git} -ri $TEXT ../chatmail/core/deltachat-jsonrpc

echo "==================== UBUNTU TOUCH USAGE ===================="
grep  --exclude-dir={.git} -ri $TEXT ../deltatouch/

