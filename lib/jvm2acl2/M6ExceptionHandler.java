import com.ibm.toad.cfparse.attributes.*;
import com.ibm.toad.cfparse.instruction.*;

public class M6ExceptionHandler {
    private int start_pc;
    private int end_pc;
    private int handler_pc;
    private String type;

    public M6ExceptionHandler(CodeAttrInfo.ExceptionInfo n, ImmutableCodeSegment imc) {
	start_pc = n.getStart();
	end_pc   = n.getEnd();
	handler_pc = n.getHandler();
	
	String tmp = n.getCatchType();
	if (tmp.equals("all"))
	    tmp = "java.lang.Throwable";

	type       = ACL2utils.JavaTypeStrToACL2TypeStr(tmp);
    };

    public String toString() {
	return "(handler " + start_pc + " " + end_pc+ "  "+handler_pc + " "+type+")";
    };

};

