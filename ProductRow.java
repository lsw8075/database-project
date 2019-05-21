import java.util.ArrayList;

public class ProductRow {
	public ArrayList<String> tnl;
	public ArrayList<RowInfo> ril;
	
	public ProductRow()
	{
		tnl = new ArrayList<String>();
		ril = new ArrayList<RowInfo>();
	}
	
	
	public void concat(String tn, RowInfo ri)
	{
		tnl.add(tn);
		ril.add(ri);
	}
	
	public ProductRow(String tn, RowInfo ri)
	{
		tnl = new ArrayList<String>();
		ril = new ArrayList<RowInfo>();
		concat(tn, ri);
	}
	
	public String toString()
	{
		String ret = "ProductRow(" + Integer.toString(tnl.size()) + ")[";
		for(int i=0; i<tnl.size(); i++)
		{
			ret += tnl.get(i) + "." + ril.get(i) + ",  ";
		}
		ret += "]";
		return ret;
	}
	
	public ProductRow clone()
	{
		ProductRow newpr = new ProductRow();
		for (int i=0; i<tnl.size(); i++)
		{
			newpr.concat(tnl.get(i), ril.get(i));
		}
		return newpr;
	}
}
