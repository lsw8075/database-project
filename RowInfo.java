import java.util.ArrayList;

public class RowInfo {
	public ArrayList<CellInfo> cells;
	String cache;
	
	public RowInfo()
	{
		cells = new ArrayList<CellInfo>();
		cache = null;
	}
	
	public RowInfo(ArrayList<CellInfo> cil)
	{
		cells = cil;
		cache = null;
	}
	
	public CellInfo get(int index) {
		return cells.get(index);
	}
	
	// warning : toString() uses cache, after changing cell call carefully 
	public String toString()
	{
		if (cache == null)
		{
			cache = "Row(" + Integer.toString(cells.size()) + ") "
					+ DBInterface.composeRow(this);
		}
		return cache;
	}
	
	public boolean equalRow(RowInfo other)
	{
		return this.toString().equals(other.toString());
	}
	
	public static ArrayList<RowInfo> intersect(ArrayList<RowInfo> al, ArrayList<RowInfo> bl)
	{
		ArrayList<RowInfo> ret = new ArrayList<RowInfo>();
		for (RowInfo arow : al)
		{
			boolean exist = false;
			for (RowInfo brow : bl)
			{
				if (arow.equalRow(brow))
				{
					exist = true;
					break;
				}
			}
			if (exist)
				ret.add(arow);
		}
		return ret;
	}
}
