import java.util.ArrayList;

// represents one table in from list
public class TableColumns {
	public String tableName;
	public String alias;
	public ArrayList<String> cols;
	
	public TableColumns(String tn, ArrayList<String> col)
	{
		tableName = tn;
		cols = col;
	}
}