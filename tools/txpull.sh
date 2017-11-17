
# this script pulls all local files from transifex and copies them to the correct directories

# before you can use this script, you have to initialize Transifex in this folder:
# tx init --user=api --pass=<your api token>
# tx set --auto-remote https://www.transifex.com/projects/p/delta-chat-android/

# common information about the Transifex CLI client can be found at:
# https://docs.transifex.com/client/

tx pull -a    # -s would also fetch the source file, this is not wanted

cp "translations/delta-chat-android.stringsxml/                            ca.xml"    ../MessengerProj/src/main/res/values-ca/strings.xml

cp "translations/delta-chat-android.stringsxml/                            de.xml"    ../MessengerProj/src/main/res/values-de/strings.xml

cp "translations/delta-chat-android.stringsxml/                            es.xml"    ../MessengerProj/src/main/res/values-es/strings.xml

cp "translations/delta-chat-android.stringsxml/                            fr.xml"    ../MessengerProj/src/main/res/values-fr/strings.xml

cp "translations/delta-chat-android.stringsxml/                            hu.xml"    ../MessengerProj/src/main/res/values-hu/strings.xml

cp "translations/delta-chat-android.stringsxml/                            it.xml"    ../MessengerProj/src/main/res/values-it/strings.xml

cp "translations/delta-chat-android.stringsxml/                            ko.xml"    ../MessengerProj/src/main/res/values-ko/strings.xml

cp "translations/delta-chat-android.stringsxml/                            nb_NO.xml" ../MessengerProj/src/main/res/values-nb/strings.xml

cp "translations/delta-chat-android.stringsxml/                            nl.xml"    ../MessengerProj/src/main/res/values-nl/strings.xml

cp "translations/delta-chat-android.stringsxml/                            pl.xml"    ../MessengerProj/src/main/res/values-pl/strings.xml

cp "translations/delta-chat-android.stringsxml/                            pt.xml"    ../MessengerProj/src/main/res/values-pt/strings.xml

cp "translations/delta-chat-android.stringsxml/                            ru.xml"    ../MessengerProj/src/main/res/values-ru/strings.xml

cp "translations/delta-chat-android.stringsxml/                            ta.xml"    ../MessengerProj/src/main/res/values-ta/strings.xml

cp "translations/delta-chat-android.stringsxml/                            te.xml"    ../MessengerProj/src/main/res/values-te/strings.xml

cp "translations/delta-chat-android.stringsxml/                            tr.xml"    ../MessengerProj/src/main/res/values-tr/strings.xml

cp "translations/delta-chat-android.stringsxml/                            uk.xml"    ../MessengerProj/src/main/res/values-uk/strings.xml

