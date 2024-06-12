RES=src/main/res
tx pull -l en
mv $RES/values-en/strings.xml $RES/values/strings.xml
rmdir $RES/values-en
./scripts/check-translations.sh
