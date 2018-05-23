
# this script pulls all files from transifex and copies them to the correct local directories

# before you can use this script, you have to initialize Transifex in this folder:
# tx init --user=api --pass=<your api token>
# tx set --auto-remote https://www.transifex.com/projects/p/delta-chat-android/

# common information about the Transifex CLI client can be found at:
# https://docs.transifex.com/client/


rm -r translations
tx pull -a -s   # -s fetches the source file, we do not copy it, but we need it for pushing back

TXPREFIX="translations/delta-chat-android.stringsxml/"
SRCPREFIX="../MessengerProj/src/main/res/values"

cp "${TXPREFIX}ca.xml"    "${SRCPREFIX}-ca/strings.xml"
cp "${TXPREFIX}da.xml"    "${SRCPREFIX}-da/strings.xml"
cp "${TXPREFIX}de.xml"    "${SRCPREFIX}-de/strings.xml"
#  "${TXPREFIX}en.xml"    "${SRCPREFIX}/strings.xml"   # we do not copy the source as the source cannot be modified at Trasifex
cp "${TXPREFIX}es.xml"    "${SRCPREFIX}-es/strings.xml"
cp "${TXPREFIX}eu.xml"    "${SRCPREFIX}-eu/strings.xml"
cp "${TXPREFIX}fr.xml"    "${SRCPREFIX}-fr/strings.xml"
cp "${TXPREFIX}hu.xml"    "${SRCPREFIX}-hu/strings.xml"
cp "${TXPREFIX}it.xml"    "${SRCPREFIX}-it/strings.xml"
cp "${TXPREFIX}ja_JP.xml" "${SRCPREFIX}-ja/strings.xml"
cp "${TXPREFIX}ko.xml"    "${SRCPREFIX}-ko/strings.xml"
cp "${TXPREFIX}nb_NO.xml" "${SRCPREFIX}-nb/strings.xml"
cp "${TXPREFIX}nl.xml"    "${SRCPREFIX}-nl/strings.xml"
cp "${TXPREFIX}pl.xml"    "${SRCPREFIX}-pl/strings.xml"
cp "${TXPREFIX}pt.xml"    "${SRCPREFIX}-pt/strings.xml"
cp "${TXPREFIX}ru.xml"    "${SRCPREFIX}-ru/strings.xml"
cp "${TXPREFIX}sq.xml"    "${SRCPREFIX}-sq/strings.xml"
cp "${TXPREFIX}sr.xml"    "${SRCPREFIX}-sr/strings.xml"
cp "${TXPREFIX}ta.xml"    "${SRCPREFIX}-ta/strings.xml"
cp "${TXPREFIX}te.xml"    "${SRCPREFIX}-te/strings.xml"
cp "${TXPREFIX}tr.xml"    "${SRCPREFIX}-tr/strings.xml"
cp "${TXPREFIX}uk.xml"    "${SRCPREFIX}-uk/strings.xml"
cp "${TXPREFIX}zh_CN.xml" "${SRCPREFIX}-zh/strings.xml"

