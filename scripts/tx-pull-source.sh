tx pull -l en
mv res/values-en/strings.xml res/values/strings.xml
rmdir res/values-en
./scrips/check-translations.sh
