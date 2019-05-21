
public class Query
{
	public static final int QUERY_SYNTAX_ERROR = 0;

	public static final int QUERY_CREATE_TABLE = 1;

	public static final int QUERY_DROP_TABLE = 2;

	public static final int QUERY_DESC = 3;

	public static final int QUERY_SELECT = 4;

	public static final int QUERY_INSERT = 5;

	public static final int QUERY_DELETE = 6;

	public static final int QUERY_SHOW_TABLES = 7;
	
	public static final int QUERY_SHOW_VALUES = 8;
	
	public static final int QUERY_DROP_VALUES = 9;

	public int type;

	public CreateTableQuery cq;

	public String dq;

	public String eq;
	
	public SelectQuery sq;
	
	public InsertQuery iq;
	
	public DeleteQuery tq;
}
