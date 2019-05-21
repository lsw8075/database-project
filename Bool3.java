
public class Bool3 {
	public static final int TRUE = 1;
	public static final int UNKNOWN = 0;
	public static final int FALSE = -1;
	
	public static final int FUTURE = 2; // determined to one of 3 values on the future, maybe.
	
	int value;
	
	public Bool3(int b)
	{
		value = b;
	}
	
	public Bool3(boolean b)
	{
		if (b)
			value = TRUE;
		else 
			value = FALSE;
	}
	
	public boolean proceed()
	{
		return value == TRUE || value == FUTURE;
	}
	
	public static Bool3 and(Bool3 a, Bool3 b)
	{
		if (a.value == FUTURE && b.value == FUTURE ||
			a.value == FUTURE && b.value == TRUE ||
			a.value == TRUE && b.value == FUTURE)
			return new Bool3(FUTURE);
		
		if (a.value == TRUE && b.value == TRUE)
			return new Bool3(TRUE);
		
		if (a.value == FALSE || b.value == FALSE)
			return new Bool3(FALSE);
		
		return new Bool3(UNKNOWN);
	}
	
	public static Bool3 or(Bool3 a, Bool3 b)
	{
		if (a.value == FUTURE && b.value == FUTURE ||
			a.value == FUTURE && b.value == FALSE ||
			a.value == FALSE && b.value == FUTURE)
			return new Bool3(FUTURE);
		
		if (a.value == TRUE || b.value == TRUE)
			return new Bool3(TRUE);
		
		if (a.value == FALSE && a.value == FALSE)
			return new Bool3(FALSE);
		
		return new Bool3(UNKNOWN);
	}
	
	public static Bool3 not(Bool3 a)
	{	
		if (a.value == TRUE)
			return new Bool3(FALSE);
		
		if (a.value == FALSE)
			return new Bool3(TRUE);
		
		return a;
	}
	
	public void and(Bool3 b)
	{
		value = and(this, b).value;
	}
	
	public void or(Bool3 b)
	{
		value = or(this, b).value;
	}
	
	public void not()
	{
		value = not(this).value;
	}
	
	public String toString()
	{
		switch(value)
		{
		case FALSE:
			return "false";
		case UNKNOWN:
			return "unknown";
		case TRUE:
			return "true";
		case FUTURE:
			return "future";
		}
		return "???";
	}
}
