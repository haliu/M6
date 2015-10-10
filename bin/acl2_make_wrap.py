#!/usr/bin/python

import os, sys, getopt, re, os.path

suffixpat = re.compile("\.cert$")


def cert(fn):
    return re.sub(suffixpat, '.cert', fn)

def acl2(fn):
    return re.sub(suffixpat, '.acl2', fn)

def lisp(fn):
    return re.sub(suffixpat, '.lisp', fn)



def usage():
    print "%s -b <top-level-make-file> <targets>" % sys.argv[0]

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "b:")
    except getopt.GetoptError:
        # print help information and exit:
        usage()
        sys.exit(2)        

    for (o, a) in opts:
        if o == '-b':
            if not a:
                usage()
                sys.exit(2)        
            base = a
        
    targets = args

    if not targets:
        usage()
        sys.exit(2)        


    for target in targets:
        rp    = os.path.realpath(target)
        rbase = os.path.realpath(base)

        pos = rp.find(rbase)

        if pos == -1:
            print "%s needs be in some subtree of %s" % (target, base)
            usage()
            sys.exit(2)

        new = rp[len(rbase)+1:]

        if os.path.exists(lisp(rp)):
            cmd = "cd %s; make %s" % (rbase, cert(new))
            print "Executing " + cmd;
            os.system(cmd)


main()
