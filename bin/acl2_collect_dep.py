#!/usr/bin/python

import os, sys, getopt, re, os.path

ACL2_EGREP_CMD_DIR = "egrep -i '^[^;]*include-book[ \t]*\".*\"'"

systempat = re.compile(":dir[ \t][ \t]*:system")
bookpat   = re.compile('\((include-book|INCLUDE-BOOK)[ \t]*"(?P<bn>.*)"')
pkgpat    = re.compile('\(in-package[ \t]*"(?P<pkg>.*)"')
suffixpat = re.compile("\.lisp$")

ACL2_PKG_CMD_DIR = "egrep -i '^[^;]*in-package[ \t]*\".*\"'"

#
# How to deal with dependencies in the .pkg files!!
#  

#
# How to deal with #+ non-standard-analysis 
#


def get_bookname(line):
    '''Get bookname for a line'''
    # ignore ":dir :system"
    # ignore ":dir" with user defined path?

    
    line = line.split(";")[0]

    if systempat.search(line):
        # if system relative ignore the dependency
        return ""

    m = bookpat.search(line)

    if m:
        return m.group('bn')

    return ""



def get_pkgname(line):
    '''Get pkg for a line'''

    line = line.split(";")[0]

    m = pkgpat.search(line)

    if m:
        return m.group('pkg')

    return ""


def step(fn):
    '''Take a book filaname find all the books it directly depends on'''
    
    print "processing %s" % fn


    fl = []

    # main body! 

    if is_pkg(fn):
        return []

    cmd = "%s %s" % (ACL2_EGREP_CMD_DIR, fn)

    output = os.popen(cmd) 
    lines = output.readlines()
    output.close()

    pathname = os.path.dirname(fn)

    for line in lines:
        bn = get_bookname(line)

        if bn:
            if pathname:
                bn = os.path.normpath(pathname + "/" + bn + ".lisp")
            else:
                bn = os.path.normpath(bn + ".lisp")
            print bn
            fl.append(bn)


    cmd = "%s %s" %  (ACL2_PKG_CMD_DIR, fn)

    if not os.path.exists(fn):
        return fl
    
    #print "GREP package" + cmd 
    output = os.popen(cmd) 
    lines = output.readlines()
    output.close()

    for line in lines:
        pkgname = get_pkgname(line)

        #print "FOUND " + pkgname + "in" + fn
         
        pkgname = pkgname.strip() 
        if pkgname:
            if pkgname!='ACL2':
                print pkgname
                fl.append(pkgname + ".pkg")

    return fl


def closure(fns):
    '''Finding all the reachable files from books in fns'''
    open = fns
    closed = {}

    while open:
        cur = open[0]
        open = open[1:]
        if closed.has_key(cur):
            pass
        else:
            next  = step(cur)
            closed[cur] = next
            open = open + next

    return closed


def usage():
    print "%s -o <depfile> <top level files>" % sys.argv[0]


def cert(fn):
    return re.sub(suffixpat, '.cert', fn)

def acl2(fn):
    return re.sub(suffixpat, '.acl2', fn)

def lisp(fn):
    return fn


def is_pkg(fn):
    return fn[-4:] == '.pkg'
    

def print_dep(outf, gdep):
    out = open(outf, "w")
    fn  = outf

    for (target, depends) in gdep.items():

        assert (is_pkg(target) or target[-5:] == '.lisp')

        if is_pkg(target):
            assert (depends == [])
            out.write("\n\n")
            continue

        else:

            out.write(cert(target)+":")
            out.write(" " + lisp(target))
            
            if os.path.exists(acl2(target)):
                out.write(" \\\n\t" + acl2(target))
                
            for dep in depends:
                out.write(" \\\n\t" + cert(dep))

            out.write("\n\n")

    out.close()

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "o:")
    except getopt.GetoptError:
        # print help information and exit:
        usage()
        sys.exit(2)        

    for (o, a) in opts:
        if o == '-o':
            if not a:
                usage()
                sys.exit(2)        
            depfn = a
        
    files = args

    if not files:
        usage()
        sys.exit(2)        


    print "Collecting dependency information from: %s" % files

    os.system("cp --backup=numbered %s %s.old" % (depfn, depfn))

    gdep = closure(files)

    f = open(depfn, "w")
    print_dep(depfn, gdep)

    #print gdep["DJVM/djvm.pkg.lisp"]

    print "Dependency information is saved into file: %s" % depfn




main()


    

