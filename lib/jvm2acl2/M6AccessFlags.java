import com.ibm.toad.cfparse.utils.Access;

class M6AccessFlags {
    
    StringBuffer flags = null;
    
    public M6AccessFlags(int f) {
	flags=new StringBuffer("(accessflags ");
	
	if (Access.isAbstract(f))
	    flags.append(" *abstract* ");

	if (Access.isClass(f))
	    flags.append(" *class* ");


	if (Access.isFinal(f))
	    flags.append(" *final* ");

	if (Access.isInterface(f))
	    flags.append(" *interface* ");

	if (Access.isNative(f))
	    flags.append(" *native* ");

	if (Access.isPrivate(f))
	    flags.append(" *private* ");


	if (Access.isProtected(f))
	    flags.append(" *protected* ");


	if (Access.isPublic(f))
	    flags.append(" *public* ");

	if (Access.isStatic(f))
	    flags.append(" *static* ");

	if (Access.isStrict(f))
	    flags.append(" *strict* ");

	if (Access.isSuper(f))
	    flags.append(" *super* ");

	if (Access.isSynchronized(f))
	    flags.append(" *synchronized* ");


	if (Access.isTransient(f))
	    flags.append(" *transient* ");

	if (Access.isVolatile(f))
	    flags.append(" *volatile* ");

	flags.append(")");
	
	//System.out.print("FLAGS" + flags);
    };
    
    public String toString(int lmargin) {
       StringBuffer padb = new StringBuffer();

       for (int i = 0; i < lmargin; i++) {
          padb.append(" ");
       }
       
       padb.append(flags.toString());

       return padb.toString();
    };
};


	
