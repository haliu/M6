/* M6Method.java:  a Java representation of an ACL2 method
 *
 * $Id: M6Method.java,v 1.12 2003/06/17 22:11:16 hbl Exp hbl $
 */

import com.ibm.toad.cfparse.*;
import com.ibm.toad.cfparse.ConstantPool.*;
import com.ibm.toad.cfparse.utils.*;
import com.ibm.toad.cfparse.instruction.*;
import com.ibm.toad.cfparse.attributes.*;

import java.lang.*;
import java.util.*;
import java.io.*;


/**
 * M6Method represents a Java method in a way that allows for easy conversion
 * to a format that ACL2 (and more specifically M6) can understand.  At the
 * heart of an M6Method is the following state information:
 * <ul>
 * <li>The name of the Method.
 * <li>A list of the parameters to the method
 * <li>A flag indicating if the method is <code>synchronized</code>
 * <li>A <code>Vector</code> of M6-formatted instructions
 * </ul>
 * <p>
 * Methods are also provided to print out the method as a String, in
 * an M6 format.
 *
 * @author George M. Porter
 * @author J Strother Moore
 * @version 1.0
 */
public class M6Method {
   private MethodInfo m;
   private ClassFile cf;

   private String  name;
   private Vector  params;
   private String  returntype;
   private boolean synced;
   private M6AccessFlags  accessflags;

   private int max_stack; // assuming there is only one code attribute for each method.
   private int max_locals;
   private int     code_length;
   private M6ExceptionHandler[]  handlers;
   private M6StackMapAttrInfo    stackmaps; 
   private LineNumberAttrInfo    lnt; 
   private LocalVariableAttrInfo lvt;


   private Vector  instructions;
   private Vector  m6instructions;
     
   private boolean debug;
   private boolean processed;


   private Hashtable tagTable; 

   private static boolean no_method_body  = false; 
   private static boolean keep_debug_info = false; 

   private String lntStr;
   private String lvtStr; 

   /**
    * This constructor takes a ClassFile and the MethodInfo
    * structure representing the method and produces an M6Method object.
    *
    * @param c	The ClassFile representing the class
    * @param mi	The MethodInfo structure representing the method to
    *           analyze.
    */
   public M6Method(ClassFile c, MethodInfo mi) {
      cf = c;
      m = mi;
      params = new Vector();
      instructions = new Vector();
      m6instructions = new Vector();
      accessflags   = null;
      handlers  = null;
      stackmaps = null;

      lnt       = null;
   
      tagTable  = new Hashtable();
      // synced= false; // as a accessflag now feb 13.
      debug = true;
      processed = false;
      lntStr = null;
      lvtStr = null;

   }

    public String getLntStr() { return lntStr; };
    public String getLvtStr() { return lvtStr; };



   private static String replaceAllandApp(String src, String o, String n) {
	StringBuffer newstr = new StringBuffer();

	int offset = 0;
	int max    = 0;
	int start; 
	
	
	max = src.indexOf(";;"); 
	if (max==-1) max = src.length();

	boolean found = false; 

	StringTokenizer tok = new StringTokenizer(src.substring(0, max));

	    while (tok.hasMoreTokens()) {
		String cur = tok.nextToken().trim();

		int y = cur.lastIndexOf("(");
		String cur1 = cur;
		if (y!=-1) 
		    cur1 = cur.substring(y+1);

		int x = cur1.indexOf(")");
		String cur2 = cur1; 
		if (x!=-1)
		    cur2 = cur1.substring(0, x);

		if (cur2.equals(o)) {
		    found = true;
		    if (y!=-1) 
			newstr.append(cur.substring(0, y+1));
		    newstr.append(n);
		    if (x!=-1)
			newstr.append(cur1.substring(x));
		}
		else 
		    newstr.append(cur);
	
		newstr.append(" ");
	    };
	    // System.out.println(newstr);
	    if (found)
		newstr.append(" ;;to "+o);

	    newstr.append(src.substring(max));

	return newstr.toString();
    };


    
    private  void resolveTagInCode() {
	// only call this after m6instructions is collected.
	Vector newinstr = new Vector();
	Vector instrs   = m6instructions;

	for (int i=0; i< instrs.size(); i++) {
	    String inst = (String) instrs.get(i);
	    Enumeration tags  = tagTable.keys();
	    for (;tags.hasMoreElements(); ) {
		Object key = tags.nextElement();
		inst = replaceAllandApp(inst, key.toString(), tagTable.get(key.toString()).toString());
	    };
	    newinstr.addElement(inst); 
	};
	
	m6instructions = newinstr;
    };

    public String printLineNumberTable() {
	StringBuffer buf = new StringBuffer();
	buf.append("((\"" + cf.getName()+"\" \"" + name+"\"");
	buf.append("\n  (");
	
	for (int i = 0; i < params.size(); i++) {
         buf.append(params.get(i));

	 if (i < (params.size() - 1)) {
	    buf.append(" ");
	 }
	}
	buf.append(")\n");
	buf.append("      (returntype . " +  returntype +"))\n");
	
	buf.append("("); 

	if (lnt!=null) {
	    int len = lnt.length();
	    for (int i=0; i< len; i++) {
		buf.append("("+lnt.getStartPC(i)+" . "+lnt.getLineNumber(i)+")\n");
	    };
	};
	buf.append(")");

	buf.append(")");

	return buf.toString();
    };

    


    public String printLocalVariableTable() {
	StringBuffer buf = new StringBuffer();
	buf.append("((\"" + cf.getName()+"\" \"" + name+"\"");
	buf.append("\n  (");
	
	for (int i = 0; i < params.size(); i++) {
         buf.append(params.get(i));

	 if (i < (params.size() - 1)) {
	    buf.append(" ");
	 }
	}
	buf.append(")\n");
	buf.append("      (returntype . " +  returntype +"))\n");
	
	buf.append("("); 

	if (lvt!=null) {
	    int len = lvt.length();
	    for (int i=0; i< len; i++) {
	        printLocalVariableEntry(buf, i);
	    };
	};
	buf.append(")");

	buf.append(")");

	return buf.toString();
    };

    private StringBuffer printLocalVariableEntry(StringBuffer buf, int i) {
	if (lvt!=null) {
	    int len = lvt.length();
	    if (i>=0 && i<len) {
		buf.append("(" + lvt.getStartPC(i)
			   + " " + lvt.getEndPC(i)
			   + " " + lvt.getVarName(i)
			   + " " + ACL2utils.JavaTypeStrToACL2TypeStr(lvt.getVarType(i)) +")\n");
	    };
	};

        return buf;
    }; 


   /**
    * returns a pretty-printed String representing this method.
    * 
    * @param lmargin	The number of spaces of indentation to place before
    *                   the output
    * @return	A pretty-printed String representing this method.
    */
   public String toString(int lmargin) {
      if (!processed) {
         return "You must call processMethod first!";
      } 

      StringBuffer buf  = new StringBuffer();
      StringBuffer padb = new StringBuffer();

      for (int i = 0; i < lmargin; i++) {
         padb.append(" ");
      }

      String pad = padb.toString();

      buf.append(pad + "(method \"" + name + "\"\n");

      buf.append(pad + "      (parameters ");
      for (int i = 0; i < params.size(); i++) {
         buf.append(params.get(i));

	 if (i < (params.size() - 1)) {
	    buf.append(" ");
	 }
      }
      buf.append(")\n");
      buf.append(pad + "      (returntype . " +  returntype +")\n");
      
      buf.append(accessflags.toString(lmargin+6)+"\n");

      //      buf.append(pad + "(accessflags ");
      //      for (int i=0; i< accessflags.size() ; i++) {
      //         buf.append((String)accessflags.get(i));
      //        if (i<accessflags.size()-1) 
      //          buf.append(" ");
      // };
      // buf.append(")\n");


      buf.append(pad +        "      (code");
      if (instructions==null) {
	  buf.append(")");
      } else {
      buf.append("\n"+pad + "           (max_stack . " + max_stack  +")" 
		     + " (max_locals . " + max_locals +")"
		     + " (code_length . " + code_length +")\n");
      buf.append(pad + "           (parsedcode\n");
   
      for (int i = 0; i < m6instructions.size(); i++) {
	  StringBuffer line = new StringBuffer(pad + "              " + m6instructions.get(i));
       
       
 
       // round the line length to multiple of 20.
       /*
       if (i<m6instructions.size()-1) {
	   int bc = 20 - (line.length()) % 20;
	   for (int j=0; j<bc; j++) 
	       line.append(" ");
       };   
	 
       if (i == (m6instructions.size() - 1)) {
	    line.append(")");
	    int bc = 20 - (line.length()) % 20;
	    for (int j=0; j<bc; j++) 
		line.append(" ");
	    line.append(";; "+i);  
	 } else {
	    line.append(";; "+i);  
	    line.append("\n");
	    }
       */ 
       buf.append(line);
       if (i<m6instructions.size()-1) 
	   buf.append("\n");
      }

      buf.append(")");



      buf.append("\n"+pad + "           (Exceptions ");
     if (handlers!=null) {
	 for (int i=0; i< handlers.length; i++) {
	     buf.append("\n"+pad + "             " + handlers[i].toString());
	 };
     };

     buf.append(")\n");

     if (stackmaps!=null) 
	 buf.append(stackmaps.toString(lmargin + 12));
     else 
	 buf.append(pad+"           (StackMap )");
     buf.append(")");
    };          
      buf.append(")"); 
     return buf.toString();
   }


    public static void setAbstractMode() {
	no_method_body = true;
    };

    public static void setKeepLntLvt() {
	keep_debug_info = true;
    };


   /**
    * This method processes the MethodInfo given in the constructor.
    * It is necessary to call this method before querying this method.
    *
    * @param constant_pool	The constant pool element from the M6Class
    *                           object representing the outer class from
    *                           which this method is taken from.
    */
   public void processMethod(Vector constant_pool) throws IOException {

      /* The name of the method */
      name = m.getName();

      if (debug)
         System.out.println("\nProcessing method: " + name);


      /* We parse in the parameters into a Vector insert "top" if necessary. if necessary. */
      String[] ps = m.getParams();
      for (int i = 0; i < ps.length; i++) {
	  String curtype = ps[i];
	  params.addElement(ACL2utils.JavaTypeStrToACL2TypeStr(curtype));
      }; 

      if (debug)
         System.out.println("Parameters: " + params);

      /* store the return type */
       returntype = ACL2utils.JavaTypeStrToACL2TypeStr(m.getReturnType()); // hanbing

      // we need to parse the Descriptor to get types, instead of using Java
      // String representation
      
      /* Set the sync flag */

      /* Set native, abstract */
      // only interested in these two flags now.  
       /*
      if (Access.isNative(m.getAccess())) {
	  accessflags.addElement("*native*");
      };

      if (Access.isStatic(m.getAccess())) {
	  accessflags.addElement("*static*");
      };

      if (Access.isAbstract(m.getAccess())) {
	  accessflags.addElement("*abstract*");
      };

      if (Access.isSynchronized(m.getAccess())) {
	  accessflags.addElement("*synced*");
      };

       */

       // Changed to make use of the M6AccessFlags 

       accessflags = new M6AccessFlags(m.getAccess());

      /* Process the code attribute */

      AttrInfoList al = m.getAttrs();
      if (al == null) {
         instructions = null;
	 return;  /* abstract method--no instructions */
      }


      CodeAttrInfo cai = (CodeAttrInfo) al.get("Code");
      if (cai==null || no_method_body) { 
	  instructions = null;
      } else {
	  MutableCodeSegment mc = new MutableCodeSegment(cf.getCP(), cai, true);
	  mc.setInstructionFactory(new StringInstructionFactory());
	  MutableCodeIterator mci = new MutableCodeIterator(mc);
	  /* mci is an iterator that gives access to the instructions */


	  int offset=0; 
	  ImmutableCodeSegment imc = new ImmutableCodeSegment(cai.getCode());
	  // ImmutableCodeIterator imci = new ImmutableCodeIterator(imc);
	  // get the parsed code part

	  // System.out.println("number of instructions" + mci.length() + " " + imc.getNumInstructions());
  	  // System.out.println("CodeSize" + cai.getCode().length + " " + " " + imc.getOffset(imc.getNumInstructions()-1));


	  while(mci.hasNext()) {
	      BaseInstruction inst = (BaseInstruction) mci.next();
	      //     System.out.println("\n"+idx+inst.toString());

	      String resultStr = parseInst(inst, cf, constant_pool, offset);
	      if (resultStr != null) {
		  m6instructions.addElement(resultStr);
		  offset = offset + inst.getLength(offset);

	      };
	  }


	  resolveTagInCode(); 
	  // resolve any tag to absolute offsets.

	  // get the max_stack, in # slot
	  max_stack = cai.getMaxStack();
      
	  // get the max_locals
	  max_locals = cai.getMaxLocals();

	  // get the code_length in number of instructions. -- changed feb 17 hanbing
	  // now in number of bytes -- the length of byte array. 
	  // instructions like tableswitch need padding.
	  code_length = cai.getCode().length; // in bytes -- feb 17 
      
	  // add an end marker to the code.
	  m6instructions.addElement("(endofcode "+code_length+")");
	  
	  // get handlers correctly.
	  CodeAttrInfo.ExceptionInfo[] exceptions = cai.getExceptions();
	  int ecount = cai.getNumExceptions(); 
      
	  if (ecount>0) {
	      handlers   = new M6ExceptionHandler[ecount];
	      for (int i=0; i<ecount; i++) {
		  M6ExceptionHandler h = new M6ExceptionHandler(exceptions[i], imc);
		  handlers[i]=h;
	      };
	  };
      
	  // get StackMap Attributes
	  AttrInfoList aal = cai.getAttrs();
	  AttrInfo sm = getStackMapAttrInfo(aal);

	  if (sm!=null) {
	      M6StackMapAttrInfo attr = new M6StackMapAttrInfo(sm, cf.getCP(), imc, max_locals);
	      stackmaps = attr;
	  } else {
	      stackmaps = null;
	  };

	  // get LineNumberAttrInfo 
	  lnt = getLineNumberAttrInfo(aal);
	  lvt = getLocalVariableAttrInfo(aal);
	  
      };
      processed = true;
      
      if (M6Method.keep_debug_info) {
	  lntStr = printLineNumberTable();
	  lvtStr = printLocalVariableTable();
      };

   };

    private static LineNumberAttrInfo getLineNumberAttrInfo(AttrInfoList al) {
	if (al==null) 
	    return null;
	
	return (LineNumberAttrInfo) (AttrUtils.getAttrByName(al, "LineNumberTable"));
    };
	
    private static LocalVariableAttrInfo getLocalVariableAttrInfo(AttrInfoList al) {
	if (al==null) 
	    return null;
	return (LocalVariableAttrInfo) (AttrUtils.getAttrByName(al, "LocalVariableTable"));
    };
			  


    private static AttrInfo getStackMapAttrInfo(AttrInfoList al) {
	if (al==null) 
	    return null;
	return AttrUtils.getAttrByName(al, "StackMap");

    };


    private static String makeClassCP(String jtype) {
	return ACL2utils.JavaTypeStrToACL2TypeStr(jtype);
    };
    
    private static String makeFieldCP(String n, String cn, String rt) 
    {
	return "(fieldCP \""+n+"\" \""+ cn + "\" " + makeClassCP(rt)+")";
    };

    private static String makeMethodCP(String n, String cn, Vector params, String rt)
    {
	StringBuffer buf = new StringBuffer();
	buf.append("(methodCP \"" + n + "\" \"" + cn + "\" (");
	
	for (int i=0; i<params.size(); i++) {
	    buf.append(makeClassCP((String)params.get(i)));
	    if (i<params.size()-1) 
		buf.append(" ");
	};
	buf.append(") ");

	buf.append(makeClassCP(rt));
	
	return buf.toString();
    };


   private String parseInst(BaseInstruction inst,
                                   ClassFile cf,
				   Vector constant_pool,
				   int offset) 
    {
      StringBuffer tmp = new StringBuffer();
      StringBuffer tmp2 = new StringBuffer();
      StringBuffer buf = new StringBuffer();
      StringTokenizer tok;
      ConstantPoolEntry ent;
      int entType;

      System.out.println(inst.toString());

      tmp.append("(" + offset + " (");

      if (inst.getTag() != null) {
	  tagTable.put(inst.getTag(), new Integer(offset));
	  tmp2.append(";;at " + inst.getTag());
      };


      /*	 
	 tmp.append("JVM::");
      */

      // here George chose not to explictly list all OPCODE, 
      // because if the inst is one byte, inst.toString() 
      // suffice

      if (inst.getLength(offset) == 1) {
	 tmp.append(inst.toString().trim());
	 tmp.append("))");  // here we handled aload_x etc.  
      } else { 

      switch (inst.getOpCode()) {

	 /* aload v5
	  * (JVM::aload 4) */
         case JVMConstants.ALOAD:
	 case JVMConstants.ASTORE:
	 case JVMConstants.ILOAD:
	 case JVMConstants.ISTORE:
	 case JVMConstants.LLOAD:
	 case JVMConstants.LSTORE:
	 case JVMConstants.FLOAD:
	 case JVMConstants.FSTORE:
         case JVMConstants.DLOAD:    
         case JVMConstants.DSTORE:    
	 case JVMConstants.BIPUSH:
	 case JVMConstants.RET:
	 case JVMConstants.SIPUSH:

	    buf.append(inst.toString().trim());
	    buf.deleteCharAt(inst.toString().trim().indexOf(" ") + 1);
	    tmp.append(buf);
	    break;

	 /* anewarray java.lang.String
	  * (JVM::anewarray (class "java.lang.String")) */
	 case JVMConstants.ANEWARRAY:
	 case JVMConstants.NEW:
	 case JVMConstants.INSTANCEOF:
   	    tok = new StringTokenizer(inst.toString().trim());
	    buf.append(tok.nextToken());
	    String typeName = tok.nextToken();
	    buf.append(" "+makeClassCP(typeName)); 
	    tmp.append(buf.toString());
	    break;

	 /* getfield Model3D java.awt.Dimention.width
	  * (JVM::getfield (fieldCP "width" "java.awt.Dimention" (class "java.awt.Dimention.Model3D")) */
	 case JVMConstants.GETFIELD:
         case JVMConstants.PUTFIELD: 
	    tok = new StringTokenizer(inst.toString().trim());
	    buf.append(tok.nextToken()); // the opcode

	    String fieldTypeName  =  tok.nextToken();             // throw away the data type
	    String field          =  tok.nextToken();
	    String fieldClassName = field.substring(0, field.lastIndexOf("."));
	    String fieldName      = field.substring(field.lastIndexOf(".")+1);

	    buf.append(" "+makeFieldCP(fieldName, fieldClassName, fieldTypeName));

	    tmp.append(buf.toString());
	    break;

	 /* goto TAG_9
	  * (JVM::goto ) */
	 case JVMConstants.GOTO:
	 case JVMConstants.IF_ACMPEQ:
	 case JVMConstants.IF_ACMPNE:
	 case JVMConstants.IF_ICMPEQ:
	 case JVMConstants.IF_ICMPNE:
	 case JVMConstants.IF_ICMPLT:
	 case JVMConstants.IF_ICMPGE:
	 case JVMConstants.IF_ICMPGT:
	 case JVMConstants.IF_ICMPLE:
	 case JVMConstants.IFEQ:
	 case JVMConstants.IFNE:
	 case JVMConstants.IFLT:
	 case JVMConstants.IFGE:
	 case JVMConstants.IFGT:
	 case JVMConstants.IFLE:
	 case JVMConstants.IFNONNULL:
	 case JVMConstants.IFNULL:
	 case JVMConstants.JSR:
	 case JVMConstants.JSR_W:

	    buf.append(inst.toString().trim());
	    //  buf.insert(buf.toString().indexOf(" ") + 1, "LABEL::");
	    tmp.append(buf.toString()); 
	    // leave the tag in the buf. 
	    break;

	 /* newarray T_INT
	  * (JVM::newarray T_INT) */
         case JVMConstants.NEWARRAY:
	     String narray = inst.toString().trim();
	     tmp.append(narray.substring(0, narray.indexOf(" "))); // op-code
	     tmp.append(" " + narray.substring(narray.lastIndexOf("_")+1));
	    break;

	 /* iinc v4 #1
	  * (JVM::iinc 4 1) */
         case JVMConstants.IINC:

	    buf.append(inst.toString().trim());
	    buf.deleteCharAt(inst.toString().trim().indexOf(" ") + 1);
	    buf.deleteCharAt(buf.toString().lastIndexOf(" ") + 1);
	    tmp.append(buf);
	    break;

	 /* multianewarray java.lang.Object[][][] #3
	  * (JVM::multianewarray 3) */
         case JVMConstants.MULTIANEWARRAY:

	    tok = new StringTokenizer(inst.toString().trim());
	    buf.append(tok.nextToken()); // the opcode
	    String ArrayType = tok.nextToken();             // throw away the data type
	    buf.append(makeClassCP(ArrayType));
	    buf.append(" ");
	    String dimCount = tok.nextToken();
	    buf.append(" ");
	    buf.append(dimCount.substring(1, dimCount.length()));
	    tmp.append(buf.toString());
	    break;
      
	 /* invokevirtual int Alpha.fun2(java.lang.Object, int)
	  * (JVM::invokevirtual "Alpha" "fun2" 2) */
         case JVMConstants.INVOKEVIRTUAL:
         case JVMConstants.INVOKESTATIC:
         case JVMConstants.INVOKESPECIAL:

	    tok = new StringTokenizer(inst.toString().trim());
	    tmp.append(tok.nextToken()); // opcode
	    String MethodReturnType = tok.nextToken(); // throw away the return type
	    String Method  = tok.nextToken("(");
	    String MethodClassName  = Method.substring(0, Method.lastIndexOf(".")).trim();
	    String MethodName       = Method.substring(Method.lastIndexOf(".") + 1).trim();
	    Vector params  = new Vector();

	    String rawparams = tok.nextToken("");

	    /* trim off the outer parens */
	    rawparams = rawparams.substring(1, rawparams.length() - 1);
	    if (rawparams.compareTo("") == 0) {
	    } else {
	       tok = new StringTokenizer(rawparams);
	       while (tok.hasMoreTokens()) {
		   params.addElement(tok.nextToken(",").trim());
	       };
	    }
	    tmp.append("\n\t\t\t\t\t" + makeMethodCP(MethodName, MethodClassName, params, MethodReturnType)+")");
	    break;

	 /* ldc "Hello, World!"
	  * ldc #65535
	  * (JVM::ldc 3)  3 points to the STRING object "Hello, World!"
	  * (JVM::ldc 5)  5 points to an INT object 65535 */
         case JVMConstants.LDC:
         case JVMConstants.LDC_W:

	     
 	     if (inst.getOpCode()==JVMConstants.LDC) {
		tmp.append("ldc ");	     
		byte cpidx1 = inst.getCode(null, 1)[1];
		int cpidx = (int) cpidx1&255;
		ent = cf.getCP().get(cpidx);
		entType = cf.getCP().getType(cpidx);
		

	    } else {
	       tmp.append("ldc_w ");

	       int cpidx1 = (int) inst.getCode(null, 1)[1];
	       cpidx1 = cpidx1 & 255;
	       int cpidx2 = (int) inst.getCode(null, 1)[2];
	       cpidx2 = cpidx2 & 255;

	       int ldc_idx = (cpidx1 << 8) | cpidx2;
	       ent = cf.getCP().get(ldc_idx);
	       entType = cf.getCP().getType(ldc_idx);
	    }

	    if (entType == ConstantPool.CONSTANT_Integer) {

	       IntegerEntry ient = (IntegerEntry) ent;
	       Integer val = new Integer(ient.getValue());

	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       };
	       
       	       tmp2.append(";;INT:: \"" + val+"\"");

	    } else if (entType == ConstantPool.CONSTANT_Float) {

	       FloatEntry fent = (FloatEntry) ent;
	       Float val = new Float(fent.getValue());

	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       };

       	       tmp2.append(";;FLOAT:: \"" + val+"\"");

	    } else if (entType == ConstantPool.CONSTANT_Double) {

	       DoubleEntry fent = (DoubleEntry) ent;
	       Double val = new Double(fent.getValue());

	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       };

       	       tmp2.append(";;Doulbe:: \"" + val+"\"");

	    } else if (entType == ConstantPool.CONSTANT_Long) {

	       LongEntry fent = (LongEntry) ent;
	       Long val = new Long(fent.getValue());

	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       };

       	       tmp2.append(";;Long:: \"" + val+"\"");

	    } else {

	       StringEntry s_ent = (StringEntry) ent;
	       String val = new String(s_ent.getAsString());
	       
	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       };


               tmp2.append(";;STRING:: \"" + val+"\"");

	    }

	    break;

	 /* ldc2_w 837462398
	  * (JVM::ldc2_w 3)  3 points to the LONG object 837462398 */
         case JVMConstants.LDC2_W:

	    tmp.append("ldc2_w ");

	    int cpidx1 = (int) inst.getCode(null, 1)[1];
	    cpidx1 = cpidx1 & 255;
	    int cpidx2 = (int) inst.getCode(null, 1)[2];
	    cpidx2 = cpidx2 & 255;

	    int ldc_idx = (cpidx1 << 8) | cpidx2;

	    ent = cf.getCP().get(ldc_idx);
	    entType = cf.getCP().getType(ldc_idx);

	    if (entType == ConstantPool.CONSTANT_Long) {

	       LongEntry lent = (LongEntry) ent;
	       Long val = new Long(lent.getValue());

	       if (constant_pool.contains(val)) {
		  tmp.append(constant_pool.indexOf(val));
	       } else {
		  constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1);  // zero-based indices
	       }

       	       tmp2.append(";; LONG:: \"" + val+"\"");

	    } else {
	       
	       DoubleEntry dent = (DoubleEntry) ent;
	       Double val = new Double(dent.getValue());

	       if (constant_pool.contains(val)) {
	          tmp.append(constant_pool.indexOf(val));
	       } else {
	          constant_pool.addElement(val);
		  tmp.append(constant_pool.size() - 1); // zero-based indices
	       };

               tmp2.append(";; DOUBLE:: \"" + val+"\"");
	    }

	    break;


	 /* Non-standard bytecode
	  * Varies machine to machine
	  * We throw them away */
	 case JVMConstants.IMPDEP1:
	 case JVMConstants.IMPDEP2:
	    
	     System.out.println("IMPDEP!!!");
	    return null;

      /* following are added by hanbing */
      /* invokeinterface <returntype> <methodClass>.<methodname>(<param>*) */
      /* (invokeinterface byte[] com.ibm.toad.cfparse.instruction.BaseInstruction.getCode(int[], int) #3 #0 )) */
      /* (JVM::ldc2_w 3)  3 points to the LONG object 837462398 */
      case JVMConstants.INVOKEINTERFACE: 
	  {
 	    tok = new StringTokenizer(inst.toString().trim());
	    tmp.append(tok.nextToken()); // opcode
	    String iMethodReturnType = tok.nextToken(); 
	    String iMethod  = tok.nextToken("(");
	    String iMethodClassName  = iMethod.substring(0, iMethod.lastIndexOf(".")).trim();
	    String iMethodName       = iMethod.substring(iMethod.lastIndexOf(".") + 1).trim();

	    Vector iparams  = new Vector();
	    String irawparams = tok.nextToken(")");
	    /* trim off the outer parens */

	    irawparams = irawparams.substring(1, irawparams.length());
	    if (irawparams.compareTo("") == 0) {
	    } else {
	       StringTokenizer itok = new StringTokenizer(irawparams);
	       while (itok.hasMoreTokens()) {
		   iparams.addElement(itok.nextToken(",").trim());
	       };
	    }

	    tok.nextToken("#"); // skip
	    String tc = tok.nextToken(" ");

	    tmp.append("\n\t\t\t\t\t" 
		       + makeMethodCP(iMethodName, iMethodClassName, iparams, iMethodReturnType)
		       + ") " + tc.substring(1));
	  }

	    break;

      case JVMConstants.CHECKCAST: 
	  { 
	      tok = new StringTokenizer(inst.toString().trim());
	      tmp.append(tok.nextToken());
	      String castType = tok.nextToken();
	      tmp.append(" "+ACL2utils.JavaTypeStrToACL2TypeStr(castType));
	  }
	  break;

      case JVMConstants.PUTSTATIC:
      case JVMConstants.GETSTATIC:
	  { 
	    tok = new StringTokenizer(inst.toString().trim());
	    buf.append(tok.nextToken()); // the opcode

	    String sfieldTypeName  =  tok.nextToken();             // throw away the data type
	    String sfield          =  tok.nextToken();
	    String sfieldClassName = sfield.substring(0, sfield.lastIndexOf("."));
	    String sfieldName      = sfield.substring(sfield.lastIndexOf(".")+1);

	    buf.append(" "+makeFieldCP(sfieldName, sfieldClassName, sfieldTypeName));
	    tmp.append(buf.toString());
	  }
	  break; 

	  /*	  lookupswitch  <count>  <(key target)>* default */ 


      case JVMConstants.LOOKUPSWITCH:
	  { /* for pupose of type checking, we only need to collect two list 
	     * key list and target list, we don't really care how those values are used.
	     * but have a execution model in ACL2 in mind. 
	     * I would chose to have a representation closer to the real JVM.  */
	      
	    tok = new StringTokenizer(inst.toString().trim());
	    tmp.append(tok.nextToken()); // the opcode
	    
	    String defaultt = tok.nextToken().trim(); // the default target
	    buf.append(defaultt);
	    

	    String count   = tok.nextToken().substring(1).trim(); // the pair count
	    int    c       = Integer.parseInt(count);
		
   	    buf.append(" "+count);

	    buf.append(" (");
	    for (int i = 0; i < c ;i++) {
		String curpair = tok.nextToken(")").trim()+")"; 

		int start = curpair.indexOf('#')+1;
		int end   = curpair.indexOf(',');
		String key     = curpair.substring(start, end);

		start = curpair.indexOf(' ')+1;
		end   = curpair.indexOf(')');
		String target  = curpair.substring(start, end);
		buf.append("(" +key+" . " + target+") ");
	    };
	    buf.setCharAt(buf.length()-1, ')');

	    tmp.append(" (lookupswitchinfo "+buf.toString()+")");
	  }
	  
	  break;


      case JVMConstants.TABLESWITCH:
	  {
	    tok = new StringTokenizer(inst.toString().trim());
	    tmp.append(tok.nextToken()); // the opcode
	    
	    String tabledefault = tok.nextToken().trim(); // the default target
	    buf.append(tabledefault);

	    String range = tok.nextToken();

	    int  s = range.indexOf('#')+1;
	    int  e   = range.lastIndexOf('-');
	    String low = range.substring(s, e);

	    int  start  = range.lastIndexOf('#')+1;
	    String high = range.substring(start);
	    
	    int l = Integer.parseInt(low);
	    int h = Integer.parseInt(high);
	    
	    buf.append(" (" + low + " . " + high+")");

	    buf.append(" (");
	    for (int i=0; i<h-l+1; i++) {
		String target = tok.nextToken().trim();
		buf.append(target+" ");
	    };
	    buf.setCharAt(buf.length()-1, ')');

	    tmp.append(" (tableswitchinfo "+ buf.toString()+")");
	  }
	    break;    
      default:
	    tmp.append("<unsupported> "+inst.toString());
      }


      tmp.append("))");
      };
       // round the line length to multiple of 20.
      if (!tmp2.equals("")) {
       int bc = 20 - tmp.length() % 20;
       for (int j=0; j<bc; j++) 
	   tmp.append(" ");
       tmp.append(tmp2); 
      };
      
      return tmp.toString().trim();
   }
}




