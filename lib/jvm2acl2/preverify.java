import java.util.*;
import java.io.*;

public class preverify {

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
	    else 
		files.addElement(cur.getPath());
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

      System.err.println("Preverify ");
      if (args.length < 3) {
	  System.err.println("Usage: preverify <classpath> <outputdirectory> <file names>* [<package names>*]\n");   
	 System.exit(0);
      }
     
           

      String[] input = new String[args.length-2];
      for (int i=0; i< args.length-2; i++)
	  input[i]=args[i+2];

      processFiles(args[0], args[1], collectFileNames(input));
   };

   private static void processFiles(String classpath, String output, String[] args) {
       String command;
       Runtime runtime = Runtime.getRuntime();
       
       for (int i =0; i < args.length; i++) {
	   String cur = args[i];
	   int    e   = cur.lastIndexOf('.');
	   if (e!=-1) {
	   String suffix   = cur.substring(e+1);
	   int    pos = cur.indexOf("/")+1; 
	   String toplevel;
	   String name;

	   if (pos !=0) {
	   // find the top level path. where the tree started.
	       toplevel = cur.substring(0, pos);
	       name   = cur.substring(pos, e);
	   } else {
	       toplevel = ".";
	       name =cur.substring(0, e);
	   };
	   File dir = new File(toplevel);
	   if (suffix.equals("class")) {
	       command ="preverify -classpath "+ classpath +" -d "+output+" "+ name; 
 	       try {
	       System.out.println("processing ..." + cur+"\n use command\n"+command+"\n");		   
	       runtime.exec(command).waitFor();
	       } catch (IOException ee) {
		   System.out.println(ee.toString());
	       } catch (InterruptedException eee) {
		   System.out.println(eee);
	       };
	   };
	   };
       };
   };
}; 




	       


