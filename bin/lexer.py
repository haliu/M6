import shlex, sys

lexer = shlex.shlex(open(sys.argv[1], "r"))
lexer.wordchars = lexer.wordchars + "+._-/"
lexer.whitespace = lexer.whitespace + "\\:"

while 1:
    token = lexer.get_token()
    if not token:
        break
    print token
