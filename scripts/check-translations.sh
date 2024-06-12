echo potential errors, if any:

RES=./src/main/res

# a space after the percent sign 
# results in an IllegalFormatException in getString()
grep --include='strings.xml' -r '\% [12]' $RES
grep --include='strings.xml' -r '\%[$]' $RES
grep --include='strings.xml' -r '\$ ' $RES
grep --include='strings.xml' -r ' \$' $RES

# check for broken usage of escape sequences:
# - alert on `\ n`, `\ N`, `\n\Another paragraph` and so on
# - allow only `\n`, `\"`, `\'` and `\’`
#   (`’` might not be escaped, but it is done often eg. in "sq", so we allow that for now)
grep --include='strings.xml' -r "\\\\[^n\"'’]" $RES

# check for usage of a single `&` - this has to be an `&amp;`
grep --include='strings.xml' -r "&[^a]" $RES

# single <br> is not needed - and not allowed in xml, leading to error "matching end tag missing"
grep --include='strings.xml' -r "<br" $RES
