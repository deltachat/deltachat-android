echo "This script allows to make changes to the english source"
echo "without forcing all translations to be redone."
echo "(on transifex the english source is the key)"
echo "This is done by pulling the translations"
echo "and immediately pushing them again together with the source."
echo "************************************************************************************"
echo "Pushing translations is POTENTIALLY HARMFUL so this script should be used with care."
echo "In most cases, just use ./scripts/tx-push-translations.sh which is safer."
echo "************************************************************************************"
read -p "Press ENTER to continue, CTRL-C to abort."
tx pull -f
tx push -s -t
./scripts/check-translations.sh
