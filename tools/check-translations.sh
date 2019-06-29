echo potential errors, if any:

# a space after the percent sign 
# results in an IllegalFormatException in getString()
grep --include='strings.xml' -r '\% ' .
grep --include='strings.xml' -r '\$ ' .
grep --include='strings.xml' -r ' \$' .
