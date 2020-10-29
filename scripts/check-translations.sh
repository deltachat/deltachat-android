echo potential errors, if any:

# a space after the percent sign 
# results in an IllegalFormatException in getString()
grep --include='strings.xml' -r '\% ' .
grep --include='strings.xml' -r '\$ ' .
grep --include='strings.xml' -r ' \$' .

# a space after a backslash is typically unwanted, sth. as `\ n`.
# (this check disallows using the backslash as such alone,
# however, this is currently no issue, as it is just not used this way anywhere)
grep --include='strings.xml' -r '\\ ' .
