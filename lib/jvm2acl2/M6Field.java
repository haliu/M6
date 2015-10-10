import com.ibm.toad.cfparse.*;
import com.ibm.toad.cfparse.utils.*;
import com.ibm.toad.cfparse.attributes.*;
import java.util.Vector;


public class M6Field { 
    private String name;
    private String type;
    private M6AccessFlags accessflags;
    private FieldInfo field;
    private int   index; // index into the constant pool

    public M6Field(ClassFile cp, FieldInfo f) {
	field = f;
	name = f.getName();
	type = ACL2utils.JavaTypeStrToACL2TypeStr(f.getType());
	accessflags = new M6AccessFlags(f.getAccess());
	index = -1; 
    };

    public void processField(Vector cp) {
	// need to add entries to constant_pool,
	// those string values.

	index = -1;
	AttrInfoList  al = field.getAttrs();
	
	if (al==null) return;

	ConstantValueAttrInfo  va = (ConstantValueAttrInfo) al.get("ConstantValue");
	if (va==null) return;
	
	int entType = va.getType();
	System.out.println(va.get());
	String value = va.get();

        if (entType == ConstantPool.CONSTANT_Integer) {
	    Integer val = Integer.valueOf(value);
	    if (cp.contains(val)) {
		index = cp.indexOf(val);
	    } else {
		cp.addElement(val);
		index = cp.size()-1;
	    };
	} else if (entType == ConstantPool.CONSTANT_Float) {
	    Float val = Float.valueOf(value);
	    if (cp.contains(val)) {
		index = cp.indexOf(val);
	    } else {
		cp.addElement(val);
		index = cp.size()-1;
	    };
	} else if (entType == ConstantPool.CONSTANT_Double) {
	    Double val = Double.valueOf(value);
	    if (cp.contains(val)) {
		index = cp.indexOf(val);
	    } else {
		cp.addElement(val);
		index = cp.size()-1;
	    };
	} else if (entType == ConstantPool.CONSTANT_Long) {
	    Long val = Long.valueOf(value);
	    if (cp.contains(val)) {
		index = cp.indexOf(val);
	    } else {
		cp.addElement(val);
		index = cp.size()-1;
	    };
	} else if (entType == ConstantPool.CONSTANT_String) {
	    String val = value;
	    if (cp.contains(val)) {
		index = cp.indexOf(val);
	    } else {
		cp.addElement(val);
		index = cp.size()-1;
	    };
	} else {

	    System.out.println("Incompatible ConstantPool ConstantValue type!");
	    System.exit(-1);
	};

	field = null; // to release some memory..
    };
	
    public String toString(int lmargin) {
      StringBuffer padb = new StringBuffer();

      for (int i = 0; i < lmargin; i++) {
         padb.append(" ");
      };
      

      padb.append("(field \"" + name + "\" "+ type +" "+accessflags.toString(0)+" " + index+")");

      return padb.toString();

    };
};
    
    


