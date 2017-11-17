
# this script pulls all local files from transifex and copies them to the correct directories

# before you can use this script, you have to initialize Transifex in this folder:
# tx init --user=api --pass=<your api token>
# tx set --auto-remote https://www.transifex.com/projects/p/delta-chat-android/

# common information about the Transifex CLI client can be found at:
# https://docs.transifex.com/client/

cp ../MessengerProj/src/main/res/values-ca/strings.xml  "translations/delta-chat-android.stringsxml/                            ca.xml"    

cp ../MessengerProj/src/main/res/values-de/strings.xml  "translations/delta-chat-android.stringsxml/                            de.xml"    

cp ../MessengerProj/src/main/res/values-es/strings.xml  "translations/delta-chat-android.stringsxml/                            es.xml"    

cp ../MessengerProj/src/main/res/values-fr/strings.xml  "translations/delta-chat-android.stringsxml/                            fr.xml"    

cp ../MessengerProj/src/main/res/values-hu/strings.xml  "translations/delta-chat-android.stringsxml/                            hu.xml"    

cp ../MessengerProj/src/main/res/values-it/strings.xml  "translations/delta-chat-android.stringsxml/                            it.xml"    

cp ../MessengerProj/src/main/res/values-ko/strings.xml  "translations/delta-chat-android.stringsxml/                            ko.xml"    

cp ../MessengerProj/src/main/res/values-nb/strings.xml  "translations/delta-chat-android.stringsxml/                            nb_NO.xml" 

cp ../MessengerProj/src/main/res/values-nl/strings.xml  "translations/delta-chat-android.stringsxml/                            nl.xml"    

cp ../MessengerProj/src/main/res/values-pl/strings.xml  "translations/delta-chat-android.stringsxml/                            pl.xml"    

cp ../MessengerProj/src/main/res/values-pt/strings.xml  "translations/delta-chat-android.stringsxml/                            pt.xml"    

cp ../MessengerProj/src/main/res/values-ru/strings.xml  "translations/delta-chat-android.stringsxml/                            ru.xml"    

cp ../MessengerProj/src/main/res/values-ta/strings.xml  "translations/delta-chat-android.stringsxml/                            ta.xml"    

cp ../MessengerProj/src/main/res/values-te/strings.xml  "translations/delta-chat-android.stringsxml/                            te.xml"    

cp ../MessengerProj/src/main/res/values-tr/strings.xml  "translations/delta-chat-android.stringsxml/                            tr.xml"    

cp ../MessengerProj/src/main/res/values-uk/strings.xml  "translations/delta-chat-android.stringsxml/                            uk.xml"    

tx push -t # should -s if we also push the source, see https://docs.transifex.com/client/push
