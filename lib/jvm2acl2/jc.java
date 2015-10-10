import java.util.*;
import java.io.*;

public class jc {

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

      System.err.println("jc");
      if (args.length < 3) {
	  System.err.println("Usage: jc <classpath> <outputdirectory> <file names>* [<package names>*]\n");   
	 System.exit(0);
      }

      String[] input = new String[args.length-2];
      for (int i=0; i< args.length-2; i++)
	  input[i]=args[i+2];

      processFiles(args[0],args[1], collectFileNames(input));
   };

   private static void processFiles(String classpath, String output, String[] args) {
       String command;
       Runtime runtime = Runtime.getRuntime();
       for (int i =0; i < args.length; i++) {
	   String cur = args[i];
	   int    e   = cur.lastIndexOf('.');
	   if (e!=-1) {
	   String suffix = cur.substring(e+1);
	   String name   = cur.substring(0, e);
	   if (suffix.equals("java")) {
	       command = "javac -g:none -target 1.1 -bootclasspath "+ classpath +" -classpath " + classpath + " -d "+output+" "+cur;
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




	       


