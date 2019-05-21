import java.util.ArrayList;

public class BoolExprInfo {
	public static final int EXPRTYPE_OR = 1;
	public static final int EXPRTYPE_AND = 2;
	public static final int EXPRTYPE_COMP_PREDICATE = 4;
	public static final int EXPRTYPE_NULL_PREDICATE = 5;
	
	public int type;
	public boolean not;
	
	public ArrayList<BoolExprInfo> bel;
	
	public CompPredicateInfo cpi;
	public NullPredicateInfo npi; // nullinfo : [tab], col
	
	public void invert() {
		not = !not;
	}
	
	public BoolExprInfo(CompPredicateInfo cp)
	{
		type = EXPRTYPE_COMP_PREDICATE;
		cpi = cp;
		not = false;
	}
	public BoolExprInfo(String tn, String cn, boolean nop)
	{
		npi = new NullPredicateInfo();
		type = EXPRTYPE_NULL_PREDICATE;
		npi.tableName = tn;
		npi.colName = cn;
		not = nop;
	}
	
	public BoolExprInfo(int t)
	{
		type = t;
		bel = new ArrayList<BoolExprInfo>();
		not = false;
	}
	
	public void add(BoolExprInfo child)
	{
		bel.add(child);
	}
	
	public int size()
	{
		return bel.size();
	}
	
	public String toString()
	{
		String snot = not ? "!" : " ";
		switch(type)
		{
		case EXPRTYPE_COMP_PREDICATE:
			return snot + "CompPred " + cpi.toString();
		case EXPRTYPE_NULL_PREDICATE:
			return snot + "NullPred " + npi.toString();
		case EXPRTYPE_OR:
			return snot + "OrPred (" + bel.size() + ")";
		case EXPRTYPE_AND:
			return snot + "AndPred (" + bel.size() + ")";
			
		}
		return "???";
	}
	
	// check & fill & eval.
	public Bool3 eval_check(ArrayList<String> tNames, ArrayList<TableColumns> froml,
			ArrayList<NameInfo> sell, int count, ProductRow pr) throws MyError
	{
		Bool3 ret, v;
		switch(type)
		{
		case EXPRTYPE_OR:
			Bool3 boolOr = new Bool3(Bool3.FALSE);
			for (BoolExprInfo child : bel)
			{
				v = child.eval_check(tNames, froml, sell, count, pr);
				if (v.value == Bool3.TRUE) // short circuit
				{
					boolOr = v;
					break;
				}
				boolOr.or(v);
			}
			ret = boolOr;
			break;
		case EXPRTYPE_AND:
			Bool3 boolAnd = new Bool3(Bool3.TRUE);
			for (BoolExprInfo child : bel)
			{
				v = child.eval_check(tNames, froml, sell, count, pr);
				if (v.value == Bool3.FALSE) // short circuit
				{
					boolAnd = v;
					break;
				}
				boolAnd.and(v);
			}
			ret = boolAnd;
			break;
		case EXPRTYPE_COMP_PREDICATE:
			ret = cpi.eval(tNames, froml, sell, count, pr);
			break;
		case EXPRTYPE_NULL_PREDICATE:
			ret = npi.eval(tNames, froml, sell, count, pr);
			break;
		default:
			throw new MyError(MyError.UnknownError);
		}
		if (not)
			ret.not();
		DBProcessor.debug(toString() + ret.toString());
		return ret;
	}
}
