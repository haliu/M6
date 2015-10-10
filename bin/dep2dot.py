#!/usr/bin/python


# assuming the dep file is target: files \
#                                  files \
#                                  files \
# ....
#

def read_until_blank(f):
    str = ""
    line = f.readline()
    while line and line[0]!='\n':
        str = str + line
        line = f.readline()

    return str.replace('\\' , ' ')

def read_dep(f):
    '''read a .dep file, until a blank line return (targets, depends)'''
    line = "\n"
    while line and line[0] == '\n':
        line = read_until_blank(f)
        fields = line.split(':')
        if len(fields) == 2:
            break

    if not line:
        return ("", [])

    return (fields[0].strip(), fields[1].split(' '))


def norm(str):
    return '"%s"' % str.strip()

def print_node(target):
    print norm(target) + ';'


def print_edge(start, end):
    print "%s -> %s;" % (norm(start), norm(end)) 

def main():

    import sys

    f = open(sys.argv[1])


    print "digraph X {"
    print 
    print 'graph [size="7,10", ratio=fill];'

    (target, depends) = read_dep(f)

    while target:
        edges = []

        print_node(target)
        for n in depends:
            n.strip()
            if n == '':
                continue
            
            if (n not in edges) and n[-5:]!='.lisp' and (n[-5:]!='.acl2'):
                print_edge(target, n)
                edges.append(n)

        (target, depends) = read_dep(f)

    print "}"

main()
                
            
        
