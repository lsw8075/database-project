import java.util.ArrayList;

public class CompPredicateInfo {
	public Operand co1;
	public String op; // one of "<" ">" "=" ">=" "<=" "!="
	public Operand co2;

	
	public CompPredicateInfo(Operand left, String mid, Operand right)
	{
		co1 = left;
		op = mid;
		co2 = right;
	}
	
	public String toString()
	{
		String s = "";
		if (co1 == null) 
			s += "future";
		else
			s += co1.toString();
		
		s += op;
		
		if (co2 == null)
			s += "future";
		else
			s += co2.toString();
		return s;
	}
	
	public Bool3 eval(ArrayList<String> tNames, ArrayList<TableColumns> froml,
			ArrayList<NameInfo> sell, int count, ProductRow pr) throws MyError {
		
		CellInfo res1 = co1.eval(tNames, froml, sell, count, pr);
		CellInfo res2 = co2.eval(tNames, froml, sell, count, pr);
		
		
		if (res1 == null || res2 == null)
		{
			DBProcessor.debug(toString() + " is future");
			return new Bool3(Bool3.FUTURE);
		}
		
		Bool3 result = res1.compareCell(res2, op);
		DBProcessor.debug(toString() + " is " + result.toString());
		
		return result;
	}
	
}
