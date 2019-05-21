import java.util.ArrayList;

public class TableElemInfo
{
	public static final int ELEM_COLUMN = 1;

	public static final int ELEM_PRIMARY = 2;

	public static final int ELEM_FOREIGN = 3;

	public int type;

	public ColumnInfo ci;

	public ArrayList < String > pi;

	public ForeignKeyInfo fi;
}

