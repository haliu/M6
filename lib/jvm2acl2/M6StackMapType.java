import java.io.*;
import com.ibm.toad.cfparse.*;

public class M6StackMapType {
    private byte    type;
    private String  type_name;
    private static  String[] type_names 
	= { "topx",  "int",  "float", "double", 
	    "long", "null", "uninitializedThis", "cpindex",
	    "uninitilizedoffset", "returnaddress" };

    public M6StackMapType(DataInputStream file, ConstantPool cp) throws IOException {
	type = file.readByte();
	
	switch (type) {
	case 0:
	case 1:
	case 2:   
	case 3:
        case 4:
	case 5:
        case 6:  
	    type_name = type_names[type];
	    break;
	case 7: 
	    { 
	      int index;
	      index = file.readUnsignedShort(); // always assume bytecode length is less than 65536
	      type_name = ACL2utils.JavaTypeStrToACL2TypeStr(cp.getAsJava(index));
	    };
	    break;
	case 8: 
	    {
	      int offset;
	      offset = file.readUnsignedShort();
	      type_name = "(uninitialized "+ offset + ")";
	    };
	    break;
	case 9: 
	    { 
		int offset;
		offset = file.readUnsignedShort();
		type_name ="(returnAddress " + offset +")";
	    };
	    break;
	default: 
	    throw new RuntimeException("Illeagal Type in StackMap"+type);
	};
    };

    private M6StackMapType(String n) {
	type_name = n;
    };

    public static M6StackMapType getTopType() {
	return new M6StackMapType("topx");
    };

    public String toString() {
	return type_name; 
    };

};








