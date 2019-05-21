
public class ColumnInfo {
	// type 0 and above are all string with len(type)
	public static final int DATATYPE_STRING = 0;
	
	public static final int DATATYPE_INVALID = -1;

	public static final int DATATYPE_INT = -2;

	public static final int DATATYPE_DATE = -3;

	public String name;

	public int type;

	public boolean notnull;

	public ColumnInfo(String n, int t) {
		name = n;
		type = t;
		notnull = false;
	}
	
}