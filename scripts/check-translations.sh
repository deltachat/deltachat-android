echo potential errors, if any:

# a space after the percent sign 
# results in an IllegalFormatException in getString()
grep --include='strings.xml' -r '\% [12]' .
grep --include='strings.xml' -r '\%[$]' .
grep --include='strings.xml' -r '\$ ' .
grep --include='strings.xml' -r ' \$' .

# check for broken usage of escape sequences:
# - alert on `\ n`, `\ N`, `\n\Another paragraph` and so on
# - allow only `\n`, `\"`, `\'` and `\’`
#   (`’` might not be escaped, but it is done often eg. in "sq", so we allow that for now)
grep --include='strings.xml' -r "\\\\[^n\"'’]" .

# check for usage of a single `&` - this has to be an `&amp;`
grep --include='strings.xml' -r "&[^a]" .
