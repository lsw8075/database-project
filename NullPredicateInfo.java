import java.util.ArrayList;

public class NullPredicateInfo {
	
	public String tableName;
	public String colName;
	String resolved_tname;
	int tIdxTC;
	int cIdxT;
	
	public NullPredicateInfo() {
		resolved_tname = null;
	}
	
	public Bool3 eval(ArrayList<String> tNames, ArrayList<TableColumns> froml,
			ArrayList<NameInfo> sell, int count, ProductRow pr) throws MyError {
		
		String msg = resolved_tname + "." + colName + "isnull";
		Bool3 result;
		// if tname is not resolved, get it
		
		if (resolved_tname == null)
		{
			Wrapper<Integer> wrapper = new Wrapper<Integer>(-1);
			tIdxTC = DBProcessor.resolveTable(tNames, froml, sell, tableName, colName, wrapper);
			resolved_tname = froml.get(tIdxTC).tableName;
			cIdxT = wrapper.val;
			if (cIdxT == -1)
				throw new MyError(MyError.UnknownError);
		}

		// if data is not ready, return future
		if (count <= tIdxTC)
		{
			DBProcessor.debug(msg + " is future");
			return new Bool3(Bool3.FUTURE);
		}
		
		// froml, product row shares same index
		RowInfo row = pr.ril.get(tIdxTC);
		CellInfo cell = row.get(cIdxT);
		result = new Bool3(cell.isNull());
	
	
		DBProcessor.debug(msg + " is " + result.toString());
		return result;
	}

}
