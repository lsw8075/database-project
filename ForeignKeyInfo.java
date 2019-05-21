import java.util.ArrayList;

public class ForeignKeyInfo
{
	public ArrayList < String > cols1;

	public String tableName;

	public ArrayList < String > cols2;

	public ForeignKeyInfo(String tn)
	{
		cols1 = new ArrayList<>();
		tableName = tn;
		cols2 = new ArrayList<>();
	}
	
	public ForeignKeyInfo(ArrayList < String > cn1, String tn, ArrayList < String > cn2)
	{
		cols1 = cn1;
		tableName = tn;
		cols2 = cn2;
	}
}