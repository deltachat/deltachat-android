# add a language, must be executed from the repo root

if [ $# -eq 0 ]
then
    echo "Please specify the language to add as the first argument (dk, ru etc.)"
    exit
fi

LANG=$1
RES=src/main/res

mkdir $RES/values-$LANG/

cp $RES/values/strings.xml $RES/values-$LANG/strings.xml

# set time to old date because transifex may have different file times
# and does not overwrite old file
# (using -t as sth. as -d "100 days ago" does not work on mac)
touch -t 201901010000 $RES/values-$LANG/strings.xml

echo "$RES/values-$LANG/strings.xml added:"
echo "- if needed, language mappings can be added to .tx/config"
echo "- pull translations using ./scripts/tx-pull-translations.sh"
echo "  (on problems, 'tx -d pull' gives verbose output)"
