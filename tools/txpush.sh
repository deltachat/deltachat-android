
# this script pulls all files from transifex and copies them to the correct local directories

# before you can use this script, you have to initialize Transifex in this folder:
# tx init --user=api --pass=<your api token>
# tx set --auto-remote https://www.transifex.com/projects/p/delta-chat-android/

# common information about the Transifex CLI client can be found at:
# https://docs.transifex.com/client/


read -p "Push all translation files to Transifex? This will OVERWRITE all changes on Transifex since the last txpull!"

TXPREFIX="translations/delta-chat-android.stringsxml/                            " # yes, there are spaces. don't know why
SRCPREFIX="../MessengerProj/src/main/res/values"

cp  "${SRCPREFIX}-ca/strings.xml"  "${TXPREFIX}ca.xml"
cp  "${SRCPREFIX}-de/strings.xml"  "${TXPREFIX}de.xml"
cp  "${SRCPREFIX}/strings.xml"     "${TXPREFIX}en.xml"  # also copy back the source, this is the only way to update the english strings
cp  "${SRCPREFIX}-es/strings.xml"  "${TXPREFIX}es.xml"
cp  "${SRCPREFIX}-fr/strings.xml"  "${TXPREFIX}fr.xml"
cp  "${SRCPREFIX}-hu/strings.xml"  "${TXPREFIX}hu.xml"
cp  "${SRCPREFIX}-it/strings.xml"  "${TXPREFIX}it.xml"
cp  "${SRCPREFIX}-ko/strings.xml"  "${TXPREFIX}ko.xml"
cp  "${SRCPREFIX}-nb/strings.xml"  "${TXPREFIX}nb_NO.xml"
cp  "${SRCPREFIX}-nl/strings.xml"  "${TXPREFIX}nl.xml"
cp  "${SRCPREFIX}-pl/strings.xml"  "${TXPREFIX}pl.xml"
cp  "${SRCPREFIX}-pt/strings.xml"  "${TXPREFIX}pt.xml"
cp  "${SRCPREFIX}-ru/strings.xml"  "${TXPREFIX}ru.xml"
cp  "${SRCPREFIX}-sq/strings.xml"  "${TXPREFIX}sq.xml"
cp  "${SRCPREFIX}-sr/strings.xml"  "${TXPREFIX}sr.xml"
cp  "${SRCPREFIX}-ta/strings.xml"  "${TXPREFIX}ta.xml"
cp  "${SRCPREFIX}-te/strings.xml"  "${TXPREFIX}te.xml"
cp  "${SRCPREFIX}-tr/strings.xml"  "${TXPREFIX}tr.xml"
cp  "${SRCPREFIX}-uk/strings.xml"  "${TXPREFIX}uk.xml"

tx push -s -t    # -s: push source, -t: push translations, -f: ignore timestamps; see https://docs.transifex.com/client/push

