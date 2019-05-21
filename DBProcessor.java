
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sleepycat.je.Database;

class TableRows
{
	public String tableName;
	public ArrayList<RowInfo> rows;
	
	public TableRows(String tn, ArrayList<RowInfo> rl)
	{
		tableName = tn;
		rows = rl;
	}
}

class DeleteCancelHint
{
	// foreign set
	public ArrayList<Integer> sfIdxSs;
	public ArrayList<Integer> sfIdxTs;
	public ArrayList<Boolean> snotnulls;
	
	public DeleteCancelHint()
	{
		sfIdxSs = new ArrayList<Integer>();
		sfIdxTs = new ArrayList<Integer>();
		snotnulls = new ArrayList<Boolean>();
	}
	
	public String toString()
	{
		String s = Integer.toString(snotnulls.size()) + "[";
		for (int i=0; i<snotnulls.size(); i++)
		{
			s += "(" + (snotnulls.get(i) ? "!" : " ")
				+ Integer.toString(sfIdxSs.get(i)) + " -> "
					+ Integer.toString(sfIdxTs.get(i)) + "), ";
		}
		return s + "]";
	}
	
	public void add(int sfIdxS, int sfIdxT, boolean snotnull)
	{
		sfIdxSs.add(sfIdxS);
		sfIdxTs.add(sfIdxT);
		snotnulls.add(snotnull);
	}
	
	public boolean nullableSet()
	{
		boolean result = true;
		for (boolean snotnull : snotnulls)
		{
			result = result & (!snotnull);
		}
		return result;
	}
}
class DeleteCancelHints
{
	public String tableName;
	public ArrayList<DeleteCancelHint> hints;
	
	public DeleteCancelHints(String tn)
	{
		tableName = tn;
		hints = new ArrayList<>();
	}
}

class DeleteInfo
{
	ArrayList<String> tables;
	ArrayList<TableColumns> from;
	
	public DeleteInfo(ArrayList<String> tl, ArrayList<TableColumns> tcl)
	{
		tables = tl;
		from = tcl;
	}
}

class SelectInfo
{
	public ArrayList<TableColumns> tableCols;
	public ArrayList<ProductRow> productRows;
	
	public ArrayList<Integer> selIdxTC;
	public ArrayList<Integer> tColIdxT;
	int len;
}

public class DBProcessor {

	DBInterface dbi;

	public DBProcessor(Database db) {
		dbi = new DBInterface(db);
	}

	// debug functions
	
	public static void debug(String string) {
		//System.out.println("[DEBUG] " + string);
	}
	
	public static<T> ArrayList<T> debugList(String str, ArrayList<T> list) {
		/*if (list != null)
		{
			System.out.print("[DEBUG] " + str + "(" + list.size() + ") = [");
			for(T item : list)
			{
				if (item == null)
					System.out.println("rawnull ");
				else
					System.out.print(item.toString() + " ");
			}		
			System.out.println("]");
		} else {
			System.out.print("[DEBUG] " + str + " list is null");
		}*/
		return list;
	}

	// util functions & funtionals
	
	@FunctionalInterface
	interface IndexLambda<T> {
		String f(T item1, int idx) throws MyError;
	}
	
	@FunctionalInterface
	interface DualLambda<T1, T2> {
		void f(T1 item1, T2 item2, int idx) throws MyError;
	}
	
	@FunctionalInterface
	interface ReduceDualLambda<T1, T2> {
		Reducible f(T1 item1, T2 item2, int idx) throws MyError;
	}
	
	interface Reducible {
		void reduce(Reducible other); 
	}

	public static <T> int findInList(ArrayList<T> al, Predicate<T> cl) {
		for (int i = 0; i < al.size(); i++) {
			if (cl.test(al.get(i)))
				return i;
		}
		return -1;
	}
	
	public static <T> int findInListCheckDup(ArrayList<T> al, Predicate<T> cl) {
		int count = 0, idx = -1;
		for (int i = 0; i < al.size(); i++) {
			if (cl.test(al.get(i)))
			{
				count++;
				idx = i;
			}
		}
		if (count > 1)
			return -count;
		
		return idx;
	}
	
	public static <T1, T2> void dual_for(ArrayList<T1> al1, ArrayList<T2> al2,
			int esizemismatch, DualLambda<T1, T2> dl) throws MyError {
		int size = al1.size();
		if (size != al2.size())
			throw new MyError(esizemismatch);
		for (int i=0; i<size; i++) {
			dl.f(al1.get(i), al2.get(i), i);
		}
	}
	
	public static <T1, T2> Reducible reduce_dual_for(ArrayList<T1> al1, ArrayList<T2> al2,
			int esizemismatch, Reducible r, ReduceDualLambda<T1, T2> dl) throws MyError {
		int size = al1.size();
		if (size != al2.size())
			throw new MyError(esizemismatch);
		for (int i=0; i<size; i++) {
			r.reduce(dl.f(al1.get(i), al2.get(i), i));
		}
		return r;
	}
	
	public static <T, R> ArrayList<R> map(List<T> tl, Function<T, R> f)
	{
		ArrayList<R> rl = new ArrayList<R>();
		rl.ensureCapacity(tl.size());
		for (T item : tl)
			rl.add(f.apply(item));
		return rl;
	}
	
	public boolean equalType(int at, int bt)
	{
		if (at >= 0 && bt >= 0)
			return true;
		return at == bt;
	}
	
	public static <T> int maxLength(String initial, ArrayList<T> tl, Function<T, String> sl)
	{
		int max_len = initial.length();
		for (T t : tl)
		{
			int len = sl.apply(t).length();
			if (max_len < len)
				max_len = len;
		}
		return max_len;
	}
	public static void repeat(String s, int num)
	{
		for (int i=0; i<num; i++)
			System.out.print(s);
	}
	
	public static<T> void prettyPrint(ArrayList<Integer> maxLens, ArrayList<T> tl) throws MyError
	{
		if (tl == null)
		{
			System.out.print("+");
			for (Integer maxLen : maxLens)
			{
				repeat("-", maxLen+2);
				System.out.print("+");
			}
		} else {
			System.out.print("|");
			dual_for(maxLens, tl, MyError.UnknownError, (maxLen, t, idx) -> {
				System.out.print(" ");
				String s = (String)t;
				System.out.print(s);
				repeat(" ", maxLen - s.length());
				System.out.print(" |");
			});
		}
		System.out.println("");
	}
	
	public static ArrayList<ProductRow> concat(Wrapper<ArrayList<ProductRow>> productRows, String tableName, RowInfo row)
	{
		if (productRows.val == null)
		{
			ArrayList<ProductRow> ret = new ArrayList<ProductRow>();
			ret.add(new ProductRow(tableName, row));
			return ret;
		}
		
		ArrayList<ProductRow> ret = new ArrayList<ProductRow>();
		for (ProductRow pr : productRows.val)
		{
			ProductRow cloned = pr.clone();
			cloned.concat(tableName, row);
			ret.add(cloned);
		}
		return ret;
	}
	
	// returns tIdxTC
	public static int resolveTable(ArrayList<String> tNames, ArrayList<TableColumns> froml,
			ArrayList<NameInfo> sell, String l_tname, String l_colname, Wrapper<Integer> cIdxT) throws MyError {

		int idx = -1, col_idx;
		if (l_tname != null) {
			debug("trying to resolve " + l_tname + "." + l_colname);
			// check tname is alias first
			
			//debug("checking tname as alias...");
			idx = findInListCheckDup(froml, (tc) -> (tc.alias != null && tc.alias.equals(l_tname)));
			
			if (idx < -1) // ambiguous alias
				throw new MyError(MyError.WhereAmbiguousReference);
			
			if (idx == -1)
			{
				//debug("checking tc...");
				// check tname is tableName
				idx = findInListCheckDup(froml, (tc) -> (tc.tableName.equals(l_tname)));
				
				if (idx < -1)
					throw new MyError(MyError.WhereAmbiguousReference);
				
				if (idx == -1)
				{
					//debug("checking if it is table...");
					// check tname is in all table name
					if (tNames.contains(l_tname))
						throw new MyError(MyError.WhereTableNotSpecified);
					
					throw new MyError(MyError.NoSuchTable);
				}	
			}
			//debug("tname is alias");
			cIdxT.val = froml.get(idx).cols.indexOf(l_colname);
			if (cIdxT.val == -1)
				throw new MyError(MyError.WhereColumnNotExist);
			
		} else {
			debug("trying to resolve " + l_colname);
			idx = -1;
			//debug("checking alias...");
			// check colname as alias first
			idx = findInListCheckDup(sell, (ni) -> (ni.alias != null && ni.alias.equals(l_colname)));
			
			if (idx < -1) // ambiguous alias
				throw new MyError(MyError.WhereAmbiguousReference);
			
			if (idx == -1)
			{
				//debug("checking tc ...");
				// find table contains colname
				idx = findInListCheckDup(froml, (tc) -> (tc.cols.contains(l_colname)));
				
				if (idx < -1)
					throw new MyError(MyError.WhereAmbiguousReference);
				
				if (idx == -1)
					throw new MyError(MyError.WhereColumnNotExist);
				
				cIdxT.val = froml.get(idx).cols.indexOf(l_colname);
				
			} else {
				//debug("name is alias, so...");
				// l_colname is an alias, maybe
				NameInfo names = sell.get(idx);
				
				// resolve colname if tableName is not specified
				// double alias like 'select id as S, S as T from student' is impossible
				if (names.tableName == null)
				{
					// find table contains colname
					idx = findInListCheckDup(froml, (tc) -> (tc.cols.contains(names.colName)));
					
					if (idx < -1)
						throw new MyError(MyError.WhereAmbiguousReference);
					
					if (idx == -1)
						throw new MyError(MyError.WhereColumnNotExist);
					
					cIdxT.val = froml.get(idx).cols.indexOf(names.colName);
				} else {
					
					// double alias like 'select S.id as T from student as S' is possible
					// check if table name is alias
					idx = findInListCheckDup(froml, (tc) -> (tc.alias != null && tc.alias.equals(names.tableName)));
					
					if (idx < -1)
						throw new MyError(MyError.WhereAmbiguousReference);
					
					if (idx == -1)
					{
						// check tname is tableName
						idx = findInListCheckDup(froml, (tc) -> (tc.tableName.equals(names.tableName)));
						
						if (idx < -1)
							throw new MyError(MyError.WhereAmbiguousReference);
						
						if (idx == -1)
						{
							// check tname is in all table name
							if (tNames.contains(names.tableName))
								throw new MyError(MyError.WhereTableNotSpecified);
							
							throw new MyError(MyError.NoSuchTable);
						}
					}
					
					cIdxT.val = froml.get(idx).cols.indexOf(names.colName);
					if (cIdxT.val == -1)
						throw new MyError(MyError.WhereColumnNotExist);
				}
			}
		}
		
		return idx;
	}
	
	// query check functions
	public void checkCreateTableQuery(CreateTableQuery cq) throws MyError {
		if (dbi.getTables().contains(cq.tableName))
			throw new MyError(MyError.TableExistenceError);

		ArrayList<String> qColNames = new ArrayList<String>();
		ArrayList<ColumnInfo> qCols = new ArrayList<ColumnInfo>();

		for (TableElemInfo tei : cq.tl) {
			if (tei.type == TableElemInfo.ELEM_COLUMN) {
				if (qColNames.contains(tei.ci.name))
					throw new MyError(MyError.DuplicateColumnDefError);

				qColNames.add(tei.ci.name);
				qCols.add(tei.ci);

				if (tei.ci.type == ColumnInfo.DATATYPE_INVALID)
					throw new MyError(MyError.CharLengthError);
			}
		}
		
		if (qCols.isEmpty())
			throw new MyError(MyError.NoColumnInTableError);
		
		boolean qPrmExist = false;
		for (TableElemInfo tei : cq.tl) {
			if (tei.type == TableElemInfo.ELEM_PRIMARY) {
				if (qPrmExist)
					throw new MyError(MyError.DuplicatePrimaryKeyDefError);

				qPrmExist = true;

				ArrayList<String> qPrmNames = new ArrayList<String>();
				for (String qPrmName : tei.pi) {
					if (!qColNames.contains(qPrmName))
						throw new MyError(MyError.NonExistingColumnDefError, qPrmName);
					if (qPrmNames.contains(qPrmName))
						throw new MyError(MyError.DuplicatedItemInPrimaryKeyError, qPrmName);
					qPrmNames.add(qPrmName);
				}
			}
		}
		// it is ok that prm key does not exist at all
		ArrayList<String> qsColNames = new ArrayList<String>();
		
		for (TableElemInfo tei : cq.tl) {
			if (tei.type == TableElemInfo.ELEM_FOREIGN) {

				if (!dbi.getTables().contains(tei.fi.tableName))
					throw new MyError(MyError.ReferenceTableExistenceError);

				List<String> mColNames = dbi.getColumnNames(tei.fi.tableName);
				List<ColumnInfo> mCols = dbi.getColumns(tei.fi.tableName);
				List<String> mPrmNames = dbi.getPrimary(tei.fi.tableName);

				if (tei.fi.cols2.size() != mPrmNames.size())
					throw new MyError(MyError.ReferenceNonPrimaryKeyError);
				
				dual_for(tei.fi.cols1, tei.fi.cols2,
						MyError.ReferenceTypeError, (qsColName, qmColName, qIdxQ) -> {
				
					if (!mPrmNames.contains(qmColName))
						throw new MyError(MyError.ReferenceNonPrimaryKeyError);

					debug("qmColName = " + qmColName);
					int qmIdxM = mColNames.indexOf(qmColName);
					if (qmIdxM == -1)
						throw new MyError(MyError.ReferenceColumnExistenceError);

					int qsIdxS = qColNames.indexOf(qsColName);
					if (qsIdxS == -1)
						throw new MyError(MyError.NonExistingColumnDefError, qsColName);

					if (!equalType(mCols.get(qmIdxM).type, qCols.get(qsIdxS).type))
						throw new MyError(MyError.ReferenceTypeError);
					
					if (qsColNames.contains(qsColName))
						throw new MyError(MyError.ReferenceMultipleExistenceError);
					
					qsColNames.add(qsColName);
				});
			}
		}
	}

	public void checkDropTableQuery(String dq) throws MyError {
		if (!dbi.getTables().contains(dq))
			throw new MyError(MyError.NoSuchTable);

		if (!dbi.getForeignSlaves(dq).isEmpty())
			throw new MyError(MyError.DropReferencedTableError, dq);
	}
	
	public InsertQuery checkInsertQuery(InsertQuery iq) throws MyError {
		if (!dbi.getTables().contains(iq.tableName))
			throw new MyError(MyError.NoSuchTable);
		
		ArrayList<ColumnInfo> tCols = dbi.getColumns(iq.tableName);
		
		ArrayList<String> tPrmNames = dbi.getPrimary(iq.tableName);
		ArrayList<ForeignKeyInfo> tForeigns = dbi.getForeignInfo(iq.tableName);
		
		ArrayList<Integer> tPrmIdxTs = map(tPrmNames,
				(tPrmName) -> {
					return findInList(tCols,
								(tCol) -> (tCol.name.equals(tPrmName))
						);
				}
		);
		
		InsertQuery reordered;
		// on cols is defined, reorder & filling column nullablity check
		if (iq.cols != null) {
			
			// index hints : table index of each query column
			ArrayList<Integer> qIdxTs = map(iq.cols,
					(qColName) -> {
						return findInList(tCols,
							(tCol) -> (tCol.name.equals(qColName))
						);
					}
				);
			reordered = new InsertQuery();
			
			reordered.tableName = iq.tableName;
			reordered.cols = new ArrayList<String>();
			reordered.vals = new ArrayList<CellInfo>();
			
			for(int i=0; i<iq.cols.size(); i++) {
				reordered.cols.add(null);
				reordered.vals.add(null);
			}
			
			dual_for(iq.cols, iq.vals, MyError.InsertTypeMismatchError,
					// find tab_col index
					(qColName, qVal, qIdxQ) -> {
					int qIdxT = qIdxTs.get(qIdxQ);
					if (qIdxT == -1)
						throw new MyError(MyError.InsertColumnExistenceError, qColName);
					// put in new order
					reordered.cols.set(qIdxT, qColName);
					reordered.vals.set(qIdxT, qVal);
			});
			
			// if null still remains in some columns, check and fill null cell
			dual_for(reordered.cols, reordered.vals, MyError.UnknownError,
					(rColName, rVal, rIdxT) -> { // rIdxT == tIdxT
						if (rColName == null)
						{
							ColumnInfo tCol = tCols.get(rIdxT);
							// set (col, val) to (col, null)
							reordered.cols.set(rIdxT, tCol.name);
							reordered.vals.set(rIdxT, new CellInfo(tCol.type, true));
						}
					});
			
		} else {
			reordered = iq;
			reordered.cols = new ArrayList<>();
			dual_for(tCols, iq.vals, MyError.InsertTypeMismatchError, (tCol, qVal, idx) -> {
				reordered.cols.add(tCol.name);
			});
		}
		
		// common constraint check by column-based lambda on query
		dual_for(tCols, reordered.vals, MyError.InsertTypeMismatchError,
				(tCol, rVal, tIdxT) -> {
			if (!equalType(rVal.type, tCol.type))
				throw new MyError(MyError.InsertTypeMismatchError);
			if (rVal.isNull() && tCol.notnull)
				throw new MyError(MyError.InsertColumnNonNullableError, tCol.name);

			// process truncation
			reordered.vals.get(tIdxT).truncate(tCol.type);
		});
		
		// foreign constraint check
		outer: for (ForeignKeyInfo tForeign : tForeigns)
		{
			// make cell checklist
			ArrayList<CellInfo> checkVals = new ArrayList<CellInfo>();
			ArrayList<String> mColNames = dbi.getColumnNames(tForeign.tableName);
			for (String mColName : mColNames)
			{
				int mIdxF = tForeign.cols2.indexOf(mColName);
				if (mIdxF == -1)
					checkVals.add(null);
				else {
					String sColName = tForeign.cols1.get(mIdxF);
					// find slave value
					int sIdxT = findInList(tCols, (tCol) -> (tCol.name.equals(sColName)));
					CellInfo sVal = reordered.vals.get(sIdxT);
					if (sVal.isNull())
						continue outer;
					checkVals.add(sVal);
				}
			}
			Wrapper<Boolean> sameRowAppears = new Wrapper<>(false);
			dbi.selectRows(tForeign.tableName, (mRow) -> {
				Wrapper<Boolean> same = new Wrapper<>(true);
				dual_for(mRow.cells, checkVals, MyError.UnknownError, (mCell, checkVal, idx) -> {
					if (checkVal != null && !mCell.equalCell(checkVal))
						same.val = false;
				});
				if (same.val)
					sameRowAppears.val = true;
				return false;
			});
			if (!sameRowAppears.val)
				throw new MyError(MyError.InsertReferentialIntegrityError);
		}
		
		// primary constraint check by row-based lambda on table
		dbi.selectRows(iq.tableName, (row) -> {
			boolean sameCell = true;
			for (int tPrmIdxT : tPrmIdxTs)
			{
				// rows are ordered in table index
				if (!row.get(tPrmIdxT).equalCell(reordered.vals.get(tPrmIdxT)))
					sameCell = false;
			}
			if (sameCell)
				throw new MyError(MyError.InsertDuplicatePrimaryKeyError);
			return false;
		});
		
		return reordered;
	}
	
	public DeleteInfo checkDeleteQuery(DeleteQuery tq) throws MyError {
		
		// get informations needed to do simulation
		ArrayList<String> tNames = dbi.getTables();
		if (!tNames.contains(tq.tableName))
			throw new MyError(MyError.NoSuchTable);
		
		ArrayList<TableColumns> froml = new ArrayList<TableColumns>();
		ArrayList<String> tColNames = dbi.getColumnNames(tq.tableName);
		froml.add(new TableColumns(tq.tableName, tColNames));
		
		ArrayList<NameInfo> emptyni = new ArrayList<>();
		
		// check by simulation of delete
		if (tq.where != null)
			dbi.selectRows(tq.tableName,
					(row) -> (tq.where.eval_check(tNames, froml, emptyni, 1, new ProductRow(tq.tableName, row)).proceed() )
			);
		
		return new DeleteInfo(tNames, froml);
	}
	
	// do translation from name to index
	public ArrayList<DeleteCancelHints> getCancelHints(TableColumns from) throws MyError {
	
		debug("Get Cancel Hints");
		
		// check delete cancellation condition
		ArrayList<String> sNames = dbi.getForeignSlaves(from.tableName);
		
		// get cancellation hints (translated as index)
		ArrayList<DeleteCancelHints> dchsl = new ArrayList<DeleteCancelHints>();
		
		for (String sName : sNames)
		{
			ArrayList<ForeignKeyInfo> sForeigns = dbi.getForeignInfo(sName);
			
			DeleteCancelHints dchs = new DeleteCancelHints(sName);
			
			for (ForeignKeyInfo sForeign : sForeigns)
			{
				if (sForeign.tableName.equals(from.tableName))
				{
					DeleteCancelHint dch = new DeleteCancelHint();
					// check slave cols are nullable
					ArrayList<ColumnInfo> sCols = dbi.getColumns(sName);
					
					for (int sfIdxSF = 0; sfIdxSF < sForeign.cols1.size(); sfIdxSF++)
					{
						final int dummy = sfIdxSF;
						int sfIdxS = findInList(sCols,
								(sCol) -> (sCol.name.equals(sForeign.cols1.get(dummy)))
							);
						if (sfIdxS == -1)
							throw new MyError(MyError.UnknownError);
						
						int sfIdxT = findInList(from.cols,
								(tColName) -> (tColName.equals(sForeign.cols2.get(dummy)))
							);

						dch.add(sfIdxS, sfIdxT, sCols.get(sfIdxS).notnull);	
					}
					dchs.hints.add(dch);
				}
			}
			dchsl.add(dchs);
		}
		return dchsl;
	}
	
	public SelectInfo checkSelectQuery(SelectQuery sq) throws MyError {
		
		ArrayList<String> tNames = dbi.getTables();
		ArrayList<String> fromNames = map(sq.from, (item) -> (item.tableName));
		
		for (String fromName : fromNames)
		{
			if (!tNames.contains(fromName))
				throw new MyError(MyError.SelectTableExistenceError, fromName);
		}
		
		ArrayList<TableColumns> fromTCs = new ArrayList<TableColumns>();
		for (NameInfo fromni : sq.from)
		{
			TableColumns fromTC = new TableColumns(fromni.tableName, dbi.getColumnNames(fromni.tableName));
			fromTC.alias = fromni.alias;
			fromTCs.add(fromTC);
		}
		
		// fill select list to all columns if null
		if (sq.sell == null)
		{
			sq.sell = new ArrayList<NameInfo>();
			for (TableColumns fromTC : fromTCs)
			{
				for (String fColName : fromTC.cols)
				{
					NameInfo ni = new NameInfo();
					ni.tableName = fromTC.tableName;
					ni.colName = fColName;
					ni.alias = null;
					sq.sell.add(ni);
				}
			}
		}
		
		// check select list & fill index list
		Wrapper<Integer> cIdxT = new Wrapper<Integer>(-1);
		int selIdxTC;
		SelectInfo si = new SelectInfo();
		si.tableCols = fromTCs;
		
		debug("Select: resolving tables...");
		si.len = 0;
		// selectinfo count is the same as select list count
		si.selIdxTC = new ArrayList<Integer>();
		si.tColIdxT = new ArrayList<Integer>();
		
		for (NameInfo ni : sq.sell)
		{
			// reuse resolveTable function
			try {
				selIdxTC = resolveTable(tNames, fromTCs, sq.sell, ni.tableName, ni.colName, cIdxT);
			} catch(MyError e) {
				throw new MyError(MyError.SelectColumnResolveError, ni.colName);
			}
			
			debug("selIdxTC : " + Integer.toString(selIdxTC) + ", tColIdxT : " + Integer.toString(cIdxT.val));
			si.selIdxTC.add(selIdxTC);
			si.tColIdxT.add(cIdxT.val);
			si.len++;
		}
		
		debug("select len is " + si.len);
		
		debug("Select: eval & check");
		
		Wrapper<ArrayList<ProductRow> > productRows = new Wrapper<>(null);
		
		// check by simulation of select
		Wrapper<Integer> count = new Wrapper<>(1);
		for (String currTableName : fromNames)
		{
			debug("This table is " + currTableName);
			ArrayList<ProductRow> selectedProductRows = new ArrayList<ProductRow>();
			dbi.selectRows(currTableName, (row) -> {
				ArrayList<ProductRow> testProductRows = concat(productRows, currTableName, row);
				for (ProductRow testProductRow : testProductRows)
				{
					debug(testProductRow.toString());
					if(sq.where.eval_check(tNames, fromTCs, sq.sell, count.val, testProductRow).proceed())
					{
						debug("add this productrow to selected productrows");
						selectedProductRows.add(testProductRow);
					}
				}
				return false;
			});
			
			productRows.val = selectedProductRows;
			count.val++;
		}
		
		si.productRows = productRows.val;
		
		return si;
	}

	// query process functions
	public void processCreateTable(CreateTableQuery cq) throws MyError {

		checkCreateTableQuery(cq);

		ArrayList<ColumnInfo> cil = new ArrayList<ColumnInfo>();
		ArrayList<String> pil = new ArrayList<String>();
		ArrayList<ForeignKeyInfo> fil = new ArrayList<ForeignKeyInfo>();

		// collect
		for (TableElemInfo tei : cq.tl) {
			switch (tei.type) {
			case TableElemInfo.ELEM_COLUMN:
				cil.add(tei.ci);
				break;
			case TableElemInfo.ELEM_PRIMARY:
				pil = tei.pi;
				break;
			case TableElemInfo.ELEM_FOREIGN:
				fil.add(tei.fi);
				break;
			}
		}

		// set columns not null if primary
		for (String item : pil) {
			int idx = findInList(cil, (col) -> (col.name.equals(item)));
			cil.get(idx).notnull = true;
		}

		dbi.putColumns(cq.tableName, cil);
		dbi.putPrimary(cq.tableName, pil);
		dbi.putForeignInfo(cq.tableName, fil);

		dbi.putTable(cq.tableName);

		System.out.println("\'" + cq.tableName + "\' table is created");
	}
	
	public void processDropTable(String dq) throws MyError {
		checkDropTableQuery(dq);
		dbi.deleteTable(dq);
		System.out.println("\'" + dq + "\' table is dropped");
	}

	// TODO : close cursor in exception in finally query
	public void processShowTables() throws MyError {

		ArrayList<String> tables = dbi.getTables();
		if (tables.isEmpty())
			throw new MyError(MyError.ShowTablesNoTable);

		System.out.println("----------------");
		for (String item : tables) {
			System.out.println(item);
		}
		System.out.println("----------------");
	}

	public void processDesc(String eq) throws MyError {
		if (!dbi.getTables().contains(eq))
			throw new MyError(MyError.NoSuchTable);

		ArrayList<ColumnInfo> cil = dbi.getColumns(eq);
		ArrayList<String> pil = dbi.getPrimary(eq);
		ArrayList<ForeignKeyInfo> fil = dbi.getForeignInfo(eq);

		String s = "table_name [" + eq + "]\ncolumn_name\t\ttype\t\tnull\t\tkey\n";

		for (ColumnInfo item : cil) {
			s += item.name + "\t\t";

			if (item.type == ColumnInfo.DATATYPE_INT)
				s += "int\t\t";
			else if (item.type == ColumnInfo.DATATYPE_DATE)
				s += "date\t\t";
			else if (item.type >= 0)
				s += "char(" + Integer.toString(item.type) + ")\t\t";
			else
				s += "invalid\t\t";

			String notnull = item.notnull ? "N" : "Y";
			s += notnull + "\t\t";

			if (pil.contains(item.name))
				s += "PRI ";

			if (findInList(fil, (fkey) -> (fkey.cols1.contains(item.name))) != -1)
				s += "FOR ";
			
			s += "\n";
		}
		System.out.println("-------------------------------------------------");
		System.out.print(s);
		System.out.println("-------------------------------------------------");
	}

	public void processInsert(InsertQuery iq) throws MyError
	{
		debugList("pre cols", iq.cols);
		debugList("pre vals", iq.vals);
		// check and get reordered insert query
		iq = checkInsertQuery(iq);
		debugList("post cols", iq.cols);
		debugList("post vals", iq.vals);
		RowInfo row = new RowInfo();
		
		dual_for(iq.cols, iq.vals, MyError.UnknownError,
			(col, val, idx) -> {
				debug("col: " + col + ", val: " + val);
				row.cells.add(val);
			}
		);
		
		dbi.insertRow(iq.tableName, row);
		
		System.out.println("The row is inserted");
	}
	
	public void processDelete(DeleteQuery tq) throws MyError
	{
		DeleteInfo di = checkDeleteQuery(tq);
		
		ArrayList<DeleteCancelHints> dchsl = getCancelHints(di.from.get(0));
		
		Wrapper<Integer> deleted = new Wrapper<Integer>(0);
		Wrapper<Integer> canceled = new Wrapper<Integer>(0);
		
		dbi.deleteRows(tq.tableName, (tRow) -> {
			debugList("Current row is", tRow.cells);
			boolean del = true;
			if (tq.where != null)
				del = tq.where.eval_check(di.tables, di.from, null, 1, new ProductRow(tq.tableName, tRow)).proceed();
			
			// using cancellation hints, check 3 conditions
			// 1
			// 2. slave column has matching cellset with deleting one
			
			// 3. if slave column cannot be null, master row delete is cancelled.
			//    if slave column can be null, slave row should be updated later.
			
			if (del)
			{
				
				for (DeleteCancelHints dchs : dchsl)
				{
					debugList("Cancel Hint Of " + dchs.tableName + " : ", dchs.hints);
					ArrayList<RowInfo> updates = new ArrayList<RowInfo>();
					Wrapper<Boolean> cancelling = new Wrapper<>(false);
					
					dbi.openDupCursor();
					dbi.deleteRows(dchs.tableName, (sRow) -> {
						boolean updateNeeded = false;
						
						
						for (DeleteCancelHint dch : dchs.hints)
						{
							Wrapper<Boolean> cancelOnCurrHint = new Wrapper<>(true);
							dual_for (dch.sfIdxSs, dch.sfIdxTs, MyError.UnknownError,
								(sfIdxS, sfIdxT, idx) -> {
									CellInfo deletingCell = tRow.get(sfIdxT);
									CellInfo slaveCell = sRow.get(sfIdxS);
									cancelOnCurrHint.val &= deletingCell.equalCell(slaveCell);
								}
							);
							if (cancelOnCurrHint.val)
							{
								if (dch.nullableSet())
								{
									sRow.cells.set(dch.sfIdxSs.get(0), new CellInfo(null));
									updateNeeded = true;
								} else {
									// do cancellation
									canceled.val++;
									cancelling.val = true;
									dbi.skip();
									break;
								}	
							}
						}
						if (updateNeeded) {
							updates.add(sRow);
							return true;
						}
						return false;
					});
					
					// do updates
					for (RowInfo update : updates)
					{
						debugList("update row : ", update.cells);
						dbi.insertRow(dchs.tableName, update);
					}
					
					dbi.closeDupCursor();
					
					if (cancelling.val)
					{
						debug("cancelling this row");
						return false;
					}
					
				}
				deleted.val++;
				debug("the row is deleted");
			}
			return del;
		});
		
		// print message
		System.out.println(deleted.val + " row(s) are deleted");
		if (canceled.val > 0)
			System.out.println(canceled.val + " row(s) are not deleted due to referential integrity");
	}
	
	public void processSelect(SelectQuery sq) throws MyError
	{
		SelectInfo si = checkSelectQuery(sq);

		ArrayList<Integer> pColMaxLenl = new ArrayList<Integer>();
		ArrayList<String> pColNames = new ArrayList<String>();
		for (int i = 0; i < si.len; i++)
		{
			int selIdxTC = si.selIdxTC.get(i);
			int tColIdxT = si.tColIdxT.get(i);
			String colName = si.tableCols.get(selIdxTC).cols.get(tColIdxT);
			int max_len = maxLength(colName, si.productRows,
					(pr) -> (pr.ril.get(selIdxTC).get(tColIdxT).toString()));
			pColMaxLenl.add(max_len);
			pColNames.add(colName);
		}
		
		// print column names
		prettyPrint(pColMaxLenl, null);
		prettyPrint(pColMaxLenl, pColNames);
		prettyPrint(pColMaxLenl, null);
		// print rows : hard coding
		for (ProductRow pr : si.productRows)
		{
			ArrayList<String> values = new ArrayList<>();
			for (int i=0; i<pColNames.size(); i++)
			{
				int selIdxTC = si.selIdxTC.get(i);
				int tColIdxT = si.tColIdxT.get(i);
				values.add( pr.ril.get(selIdxTC).get(tColIdxT).toString());
			}
			prettyPrint(pColMaxLenl, values);
		}
		prettyPrint(pColMaxLenl, null);
	}
	
	public void processShowValues() throws MyError {
		dbi.device.getSub("", (ks, ds) -> (false));
	}
	
	public void processDropValues() throws MyError {
		dbi.device.deleteSub("", null);
		System.out.println("Cleared database!");
	}
	
	public boolean processQuery(ArrayList<Query> ql) {

		try {
			for (Query q : ql) {
				dbi.openCursor();
				switch (q.type) {
				case Query.QUERY_CREATE_TABLE:
					processCreateTable(q.cq);
					break;
				case Query.QUERY_DROP_TABLE:
					processDropTable(q.dq);
					break;
				case Query.QUERY_DESC:
					processDesc(q.eq);
					break;
				case Query.QUERY_SHOW_TABLES:
					processShowTables();
					break;
				case Query.QUERY_INSERT:
					processInsert(q.iq);
					break;
				case Query.QUERY_DELETE:
					processDelete(q.tq);
					break;
				case Query.QUERY_SELECT:
					processSelect(q.sq);
					break;
				case Query.QUERY_SHOW_VALUES:
					processShowValues();
					break;
				case Query.QUERY_DROP_VALUES:
					processDropValues();
					break;
				}
				dbi.closeCursor();
			}
		} catch (MyError e) {
			dbi.closeCursor();
			System.out.println(e.toString());
			return true;
		}
		return false;
	}
}
