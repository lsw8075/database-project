import java.util.ArrayList;

public class SelectQuery {
	ArrayList<NameInfo> sell; // tab, col, as. if null, *
	ArrayList<NameInfo> from; // tab, as.
	BoolExprInfo where; // can be null
}
