import java.util.Vector;
public class ACL2utils {
    public static String JavaTypeStrToACL2TypeStr(String jtype) {
	//jst do simple changes
	// 1. enclose class name with " ", change into (class "<classname>") form
	// 2. change <anystr>[] into (array trans(<anystr>))
	
	int i = jtype.lastIndexOf("[]"); 
	
	if (i!=-1)
	    return "(array "+JavaTypeStrToACL2TypeStr(jtype.substring(0,i))+ ")";
	else if (isPrimitiveType(jtype))
		return jtype;
	else return "(class \""+jtype + "\")";
    };

    private static boolean isPrimitiveType(String jtype) {
	if (  jtype.equals("byte")||jtype.equals("int")
	    ||jtype.equals("char")||jtype.equals("double")
	    ||jtype.equals("float")||jtype.equals("long")
	    ||jtype.equals("boolean")||jtype.equals("short")
	    ||jtype.equals("void"))
	    return true;
	return false;
    };

    public static int typeSize (String typename) {
	if (typename.equals("long")||(typename.equals("double")))
	    return 2;
	else
	    return 1;
    };

	
    public static M6StackMapType[] expandTypeList(M6StackMapType[] l){
	Vector nl = new Vector();
	
	M6StackMapType top = M6StackMapType.getTopType();

	for (int i=0; i<l.length; i++){
	    nl.addElement(l[i]);
	    if (typeSize(l[i].toString())==2)
		nl.addElement(top);
	};
	
	Object[] ll = nl.toArray(); 
	M6StackMapType[] ret = new M6StackMapType[ll.length];

	for (int i=0; i<ll.length; i++){
	    ret[i] = (M6StackMapType) ll[i];
	};
	
	return ret;
    };

    public static M6StackMapType[] expandToLength(M6StackMapType[] l, int len) {
	M6StackMapType[] nl = new M6StackMapType[len];
	
	int i=0;
	
	try {
	for (; i<l.length ; i++)
	    nl[i] = l[i];
	} catch (ArrayIndexOutOfBoundsException e) {
	    System.out.println("M6StackMapType!");
	};

	for (;i<len; i++)
	    nl[i]= M6StackMapType.getTopType();
	
	return nl;
    };



};
    
