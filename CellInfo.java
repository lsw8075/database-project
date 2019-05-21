
public class CellInfo {
	public int type; // if null, default type is int...
	public String data; // string / date / int
	public int int_data;

	public CellInfo(String s, boolean isDate) {
		if (isDate)
		{
			type = ColumnInfo.DATATYPE_DATE;
			data = s;
		} else {
			type = 0; // TODO : string type check
			data = s.substring(1, s.length()-1);
		}
		
	}
	
	// for typed null
	public CellInfo (int t, boolean dummy)
	{
		type = t;
		data = null;
	}
	
	public void truncate(int t)
	{
		if (t > 0 && data.length() > t)
			data = data.substring(0, t);
	}

	public CellInfo(Integer i) {
		type = ColumnInfo.DATATYPE_INT;

		if (i == null)
			data = null;
		else {
			data = i.toString();
			int_data = i;
		}
	}

	public boolean isNull() {
		return data == null;
	}

	public String encode() {

		if (data == null) {
			return "%0";
		}

		if (type == ColumnInfo.DATATYPE_INT)
			return "%1" + data;
		else if (type == ColumnInfo.DATATYPE_DATE)
			return "%3" + data;

		String ret = "%2";
		for (int i = 0; i < data.length(); i++) {
			if (data.charAt(i) == '%') {
				ret += "%5";
			} else if (data.charAt(i) == '/') {
				ret += "%4";
			} else {
				ret += data.charAt(i);
			}
		}
		return ret;
	}

	public void decode(String code) {
		if (code.equals("%0")) {
			// default is int
			type = ColumnInfo.DATATYPE_INT;
			data = null;
			return;
		}

		data = code.substring(2);

		if (code.startsWith("%1")) {
			type = ColumnInfo.DATATYPE_INT;
			data = code.substring(2);
			int_data = Integer.parseInt(data);
		} else if (code.startsWith("%3")) {
			type = ColumnInfo.DATATYPE_DATE;
			data = code.substring(2);
		} else {
			type = 0;
			data = "";
			for (int i = 2; i < code.length(); i++) {
				if (code.charAt(i) == '%') {
					i++;
					if (code.charAt(i) == '4')
						data += "/";
					else
						data += "%";

				} else
					data += code.charAt(i);
			}
		}
	}
	
	public CellInfo(String code, int dummy)
	{
		decode(code);
	}
	
	// used in non-where
	public boolean equalCell(CellInfo c) throws MyError {
		
		if (type != c.type)
			throw new MyError(MyError.UnknownError);
		DBProcessor.debug("equalCell " + data + " == " + c.data + " is " + data.equals(c.data));
		
		return data.equals(c.data);
	}
	
	// used in where
	public Bool3 compareCell(CellInfo c, String op) throws MyError {
		boolean ret = false;
		
		if (isNull() || c.isNull())
			return new Bool3(Bool3.UNKNOWN);
		
		if (type != c.type)
			throw new MyError(MyError.WhereIncomparableError);
		
		if (type == ColumnInfo.DATATYPE_INT)
		{
			switch(op)
			{
			case ">":
				ret = int_data > c.int_data;
				break;
			case "<":
				ret = int_data < c.int_data;
				break;
			case "=":
				ret = int_data == c.int_data;
				break;
			case "!=":
				ret = int_data != c.int_data;
				break;
			case ">=":
				ret = int_data >= c.int_data;
				break;
			case "<=":
				ret = int_data <= c.int_data;
				break;
			default:
				throw new MyError(MyError.UnknownError);
			}

		} else { // Date, String
			switch(op)
			{
			case ">":
				ret = data.compareTo(c.data) > 0;
				break;
			case "<":
				ret = data.compareTo(c.data) < 0;
				break;
			case "=":
				ret = data.compareTo(c.data) == 0;
				break;
			case "!=":
				ret = data.compareTo(c.data) != 0;
				break;
			case ">=":
				ret = data.compareTo(c.data) >= 0;
				break;
			case "<=":
				ret = data.compareTo(c.data) <= 0;
				break;
			default:
				throw new MyError(MyError.UnknownError);
			}
		}
		return new Bool3(ret);
	}
	
	public String toString()
	{
		if (data == null) {
			return "null";
		}
		return data;
	}
}
