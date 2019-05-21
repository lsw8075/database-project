import java.util.ArrayList;

public class Operand {
	public String tableName; // can be null, filled later.
	public String colName;
	public CellInfo ci;
	public CellInfo last_result;
	
	// in case table data is used;
	String resolved_tname;
	int tIdxTC;
	int cIdxT;
	
	public Operand(CellInfo cell)
	{
		tableName = colName = null;
		ci = cell;
		
		resolved_tname = null;
	}
	
	public Operand(String tn, String cn)
	{
		tableName = tn;
		colName = cn;
		ci = null;
		resolved_tname = null;
	}
	
	
	public CellInfo eval(ArrayList<String> tNames, ArrayList<TableColumns> froml,
			ArrayList<NameInfo> sell, int count, ProductRow pr) throws MyError {
		if (ci == null)
		{
			// try to get cellinfo from product row data
			
			// if tname is not resolved, get it
			
			if (resolved_tname == null)
			{
				Wrapper<Integer> wrapper = new Wrapper<Integer>(-1);
				tIdxTC = DBProcessor.resolveTable(tNames, froml, sell, tableName, colName, wrapper);
				resolved_tname = froml.get(tIdxTC).tableName;
				cIdxT = wrapper.val;
				if (cIdxT == -1)
					throw new MyError(MyError.UnknownError);
				DBProcessor.debug("count is " + count + ", index resolved : " + resolved_tname + "=" + Integer.toString(tIdxTC)
						+ ", " + colName + "=" + Integer.toString(cIdxT));
			}

			// if data is not ready, return null to return future
			if (count <= tIdxTC)
				return null;
			
			// froml, product row shares same index
			RowInfo row = pr.ril.get(tIdxTC);
			last_result = row.get(cIdxT);
			return last_result;
		}
		return ci;
	}
	
	public String toString()
	{
		if (ci != null)
			return ci.toString();
		String ret = "(" + resolved_tname + "." + colName;
		if (last_result != null)
			ret += "=" + last_result.toString();
		ret += ")";
		return ret;
	}
}
