// this program write to /var/local/hbl/....
// java jvm2acl2 some/path/tablename <.class files> <directories>
// it generate separate files for each .class file.
// and a file call table-name-class-table with lisp forms that load all class.
//
// it also take an -a argument that tells it to ignore method-body
//

import com.ibm.toad.cfparse.*;
import com.ibm.toad.cfparse.utils.*; 
import java.util.*;
import java.io.*;

public class jvm2acl2x {

    private static boolean abstract_mode = false;
    private static boolean keep_lntlvt   = false;

    private static String  defaultpath = "/var/local/";

    private static String  usage = 
	"Usage: jvm2acl2x [-a] [-g] [-d default-path-prefix] <output directory> <file names>* [<directory names>*]\n";

    private static int level =0;
    private static String[] collectFileNames (String[] args) {
	Vector files = new Vector();
	String[] reslt = null;
	try {
	for (int i=0; i<args.length; i++) {
	    File cur = new File(args[i]);
	    System.out.println(args[i]);
	    if (cur.isDirectory()) {
		String[] nextlevel=cur.list(); 
		for (int j=0; j<nextlevel.length; j++) {
		    nextlevel[j]=(cur.getPath()+"/"+nextlevel[j]);
		};
		String[] names = collectFileNames(nextlevel);
		for (int j=0; j<names.length; j++) {
		    files.addElement(names[j]);
		};
	    } 
	    else {
		String curn = cur.getPath();
		int    e   = curn.lastIndexOf('.');
		if (e!=-1) {
		    String suffix   = curn.substring(e+1);
		    if (suffix.equals("class"))
			files.addElement(curn);
		};
	    };	
	};
	
	reslt = new String[files.size()];
	for (int i=0; i<files.size(); i++) {
	    reslt[i]=(String) files.get(i);
	    System.out.println(reslt[i]);
	};
	} catch (Exception e) {
	    System.out.println("File reading error!");
	};
	return reslt;
    };

   public static void main(String[] args) {
       
       int     start = 0; 
       String  output;  


       System.err.println("JVM --> ACL2  An automatic M6 state generator.");

       if (args.length <= 1) {
	   System.err.println(usage);
	 System.exit(0);
       };
       
       start = 0;

       if (args[start].equals("-a")) {
	   abstract_mode = true;
	   M6Method.setAbstractMode();
	   start++;
	   if (args.length - start < 2) {
	       System.err.println(usage);
	       System.exit(0);
	   }; 
       }; 	   

       if (args[start].equals("-g")) {
	   keep_lntlvt = true;
	   M6Method.setKeepLntLvt();
	   M6Class.setKeepLntLvt();
	   
	   start++;
	   if (args.length - start < 2) {
	       System.err.println(usage);
	       System.exit(0);
	   }; 
       }; 	   

       if (args[start].equals("-d")) {
	   start++;
	   if (args.length - start  < 3) {
	       System.err.println(usage);
	       System.exit(0);
	   }; 
	   defaultpath = args[start];
	   start++;

	   if (defaultpath.charAt(defaultpath.length()-1)!='/')
	       defaultpath = defaultpath + "/";
	      
       }; 	   

       output = args[start];

       String[] input = new String[args.length - start-1];
       for (int i=start; i< args.length-1; i++)
       	  input[i-start]=args[i+1];

       // String[] input = new String[1];

       // input[0]="Demo.class";

      processFiles(output, collectFileNames(input));
      //  processFiles(output, input);

   };

    private static String processOneFile (String classfn, String pathn, StringBuffer table) {
	try {
	 ClassFile cfs = (ClassFile) new ClassFile(classfn);
	 M6Class   curClass = new M6Class(cfs);
         curClass.processClassFile();

	 StringBuffer ctBuf = new StringBuffer();
	 String classname = cfs.getName();

	 if (abstract_mode) 
	     classname = classname+"-abs";

	 ctBuf.append("(defconst *" + classname + "*\n");
	 ctBuf.append(" (make-class-def\n");
	 ctBuf.append("  ");
	 ctBuf.append(curClass.toString(4));
	 ctBuf.append("))\n\n");
	 
	 writeToFile(pathn+"/*"+classname+"*.lisp", ctBuf);
	 
	 if (keep_lntlvt) {
	     writeToFile(pathn+"/*"+classname+"*.lnt.lisp", curClass.getlntdesc());
	     writeToFile(pathn+"/*"+classname+"*.lvt.lisp", curClass.getlvtdesc());
	 };

	 return classname;

	} catch (Exception e) {
	    System.err.println(e);
	    System.exit(-1);
	};

	return null;
    }

   private static void processFiles(String tname, String[] args) {  
       StringBuffer ctBuf   = new StringBuffer();
       StringBuffer ctBuf2  = new StringBuffer();
       StringBuffer mapName2P = new StringBuffer();       
       String tablename, tablepath;
      
       if (tname.lastIndexOf('/')!=-1) {
	   tablename = tname.substring(tname.lastIndexOf('/')+1);    
	   tablepath = tname.substring(0, tname.lastIndexOf('/'));
       } else {
	   tablename = tname;
	   tablepath = tname;
       };
       

      ctBuf.append(";; load all class description as Lisp constants\n"); 
      
      if (abstract_mode) 
	  ctBuf2.append("(defconst *"+tablename+"-class-table-abs*\n");
      else 
	  ctBuf2.append("(defconst *"+tablename+"-class-table*\n");

      ctBuf2.append("  (make-class-table1 \n");
      mapName2P.append("(defconst *package-name-map* \n"); 

      /* We parse each of the classfiles in order */
      for (int i = 0; i < args.length; i++) {
         try {
	     String classname = processOneFile(args[i], tablepath, ctBuf2);
	     ctBuf.append("(ld \"*" + classname + "*.lisp\")\n");  

	     int offset = classname.lastIndexOf('.');
	     String packagename=null;
	     if (offset== -1) 
		 packagename = "";
	     else 
		 packagename = classname.substring(0, offset);
	     mapName2P.append("  (\"" + classname + "\" . \"" + packagename + "\")");


	     ctBuf2.append("   *" + classname + "*");	     
	     if (i<args.length-1) 
		 mapName2P.append("\n");
		 ctBuf2.append("\n");
	 } catch (java.lang.Exception e) {
	    System.err.println("Could not open file " + args[0] + " "+e);
	    System.exit(0);
	 }
      }
      
      ctBuf2.append("))\n\n");
      mapName2P.append(")\n");
      
      ctBuf.append("\n\n"+ctBuf2+mapName2P);

      if (abstract_mode) 
	  writeToFile(tablepath+'/'+tablename + "-class-table-abs.lisp", ctBuf);
      else 
	  writeToFile(tablepath+'/'+tablename + "-class-table.lisp", ctBuf);

   };

    private static void writeToFile(String fn, StringBuffer ctBuf) {
      System.out.println("Writing state to file " + defaultpath + fn);
      try {
	  FileWriter outfile = new FileWriter(defaultpath+fn);

	  outfile.write("; " + fn + "\n");
	  outfile.write("; Automatically generated by jvm2acl2x ");

	  Calendar rightNow = Calendar.getInstance();

	  outfile.write("on " + rightNow.getTime().toString() + ".\n;\n\n");

	  outfile.write(ctBuf.toString() + "\n");
	  outfile.write("\n");
	  outfile.flush();
	  outfile.close();

	  System.out.println("done!");

      } catch (IOException e) {
         System.out.println("\nError writing to " +  fn );
	 System.exit(1);
      }
   }
}




