import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.sleepycat.je.Database;

class Pair {
	public String s0;
	public String s1;
	public Pair(String a0, String a1)
	{
		s0 = a0; s1 = a1;
	}
}

class Cache<T> {
	ArrayList<String> tables;
	ArrayList<T> caches;
	
	public Cache()
	{
		tables = new ArrayList<String>();
		caches = new ArrayList<T>();
	}
	
	public T get(String tableName)
	{
		return null;
		/*
		int idx = tables.indexOf(tableName);
		if (idx != -1)
			return caches.get(idx);
		return null;*/
	}
	
	public boolean set(String tableName, T data)
	{
		int idx = tables.indexOf(tableName);
		if (idx != -1)
		{
			caches.set(idx, data);
			return true;
		}
		tables.add(tableName);
		caches.add(data);
		return false;
	}
}

public class DBInterface
{
	public DBDevice device;
	
	// cache data
	Cache<ArrayList<String> > cacheTables;
	Cache<ArrayList<String> > cacheColNames;
	Cache<ArrayList<ColumnInfo> > cacheColumns;
	Cache<ArrayList<String> > cachePrimarys;
	Cache<ArrayList<ForeignKeyInfo> > cacheForeigns;
	Cache<ArrayList<String> > cacheForeignSlaves;
	
	public DBInterface(Database db)
	{
		device = new DBDevice(db);
	}
	
	public void openCursor()
	{
		device.openCursor();
		// make cache
		cacheTables = new Cache<>();
		cacheColNames = new Cache<>();
		cacheColumns = new Cache<>();
		cachePrimarys = new Cache<>();
		cacheForeigns = new Cache<>();
		cacheForeignSlaves = new Cache<>();
	}

	public void closeCursor()
	{
		device.closeCursor();
	}
	
	public void openDupCursor()
	{
		device.openDupCursor();
	}
	
	public void closeDupCursor()
	{
		device.closeDupCursor();
	}
	
	public void skip()
	{
		device.skip();
	}
	
	@FunctionalInterface
	interface DataLambda<T> {
		T f(int idx, String name) throws MyError;
	}
	
	@FunctionalInterface
	interface TransformLambda<T> {
		T f(int idx, String name, T input) throws MyError;
	}
	
	@FunctionalInterface
	interface RowLambda {
		boolean f(RowInfo row) throws MyError;
	}
	
	// key name convention & value separation functions

	static String make(String name0, String name1)
	{
		return name0 + "/" + name1;
	}

	static String make(String name0, String name1, String name2)
	{
		return name0 + "/" + name1 + "/" + name2;
	}

	static String make(ArrayList< String > al)
	{
		if (al.size() < 1) return "";
		return String.join("/", al);
	}
	
	static List<String> split(String s)
	{
		return Arrays.asList(s.split("/"));
	}
	
	static String makeIdx(String made, int idx)
	{
		return made + "/" + Integer.toString(idx);
	}
	
	static String makeSubKey(String made)
	{
		return made + "/";
	}
	
	static String makeTables()
	{
		return make("", "tables");
	}
	
	static String makeColumns(String tableName)
	{
		return make(tableName, "column");
	}
	
	static String makeColumnType(String tableName, String idxs)
	{
		return make(tableName, "type", idxs);
	}
	
	static String makeColumnNotNull(String tableName, String idxs)
	{
		return make(tableName, "notnull", idxs);
	}
	
	static String makePrimary(String tableName)
	{
		return make(tableName, "primary");
	}
	
	static String makeSlave(String tableName)
	{
		return make(tableName, "slave");
	}
	
	static String makeMaster(String tableName)
	{
		return make(tableName, "master");
	}
	
	static String makeRow (String tableName)
	{
		return make(tableName, "row");
	}

	// parse (idx, data) pair and return idx-sorted collection
	<T> ArrayList<T> getSorted(String key, DataLambda<T> l) throws MyError
	{
		ArrayList<T> result = new ArrayList<>();
		
		device.get(key, (ks, data) -> {
			List<String> pair = split(data);
			int idx = Integer.parseInt(pair.get(0));
			String name = pair.get(1);
			
			while (result.size() <= idx) result.add(null);
			result.set(idx, l.f(idx, name));
			return false;
		});
		return result;
	}
	
	// get additionals
	<T> ArrayList<T> getAdditional(ArrayList<T> list, String key, TransformLambda<T> l) throws MyError
	{
		device.getSub(key, (ks, data) -> {
			int idx = Integer.parseInt(ks.substring(key.length()));
			list.set(idx, l.f(idx, data, list.get(idx)));
			return false;
		});
		return list;
	}
	
	public ArrayList<String> getTables() throws MyError
	{
		ArrayList<String> cache = cacheTables.get("all");
		if (cache == null)
		{
			cache = device.get(makeTables(), null);
			cacheTables.set("all", cache);
		}
		return cache;
	}
	
	public void putTable(String tableName)
	{
		device.put(makeTables(), tableName);
	}

	public ArrayList<String> getColumnNames(String tableName) throws MyError
	{
		ArrayList<String> data = cacheColNames.get(tableName);
		if (data == null)
		{
			data = getSorted(makeColumns(tableName), (idx, name) -> (name));
			cacheColNames.set(tableName, data);
		}
		return data;
	}
	
	public ArrayList<ColumnInfo> getColumns(String tableName) throws MyError
	{
		ArrayList<ColumnInfo> data= cacheColumns.get(tableName);
		if (data == null)
		{
			data = getSorted(makeColumns(tableName), (idx, name) -> (new ColumnInfo(name, 0)) );
			
			getAdditional(data, makeColumnType(tableName, ""), (idx, ds, input) -> {
				input.type = Integer.parseInt(ds);
				return input;
			});
			
			getAdditional(data, makeColumnNotNull(tableName, ""), (idx, ds, input) -> {
				input.notnull = ds.equals("Y");
				return input;
			});
			
			cacheColumns.set(tableName, data);
		}
		return data;
	}
	
	public void putColumns(String tableName, ArrayList<ColumnInfo> lc)
	{
		for (int i=0; i<lc.size(); i++)
		{
			ColumnInfo col = lc.get(i);
			String idxs = Integer.toString(i);
			device.put(makeColumns(tableName), make(idxs, col.name));
			device.put(makeColumnType(tableName, idxs), Integer.toString(col.type));
			device.put(makeColumnNotNull(tableName, idxs), col.notnull ? "Y" : "N");
		}
	}
	
	public ArrayList<String> getPrimary(String tableName) throws MyError
	{
		ArrayList<String> primary = cachePrimarys.get(tableName);
		if (primary == null)
		{
			primary = new ArrayList<String>();
			primary = device.get(makePrimary(tableName), null);
			cachePrimarys.set(tableName, primary);
		}
		return primary;
	}
	
	public void putPrimary(String tableName, ArrayList<String> primary)
	{
		for (String item : primary) {
			device.put(makePrimary(tableName), item);
		}
	}
	
	public ArrayList<String> getForeignSlaves(String tableName) throws MyError
	{
		ArrayList<String> slaves = cacheForeignSlaves.get(tableName);
		if (slaves == null)
		{
			slaves = device.get(makeSlave(tableName), null);
			cacheForeignSlaves.set(tableName, slaves);
		}
		return slaves;
	}
	
	public ArrayList<ForeignKeyInfo> getForeignInfo(String tableName) throws MyError
	{
		ArrayList<ForeignKeyInfo> cache = cacheForeigns.get(tableName);
		if (cache == null) {
			
			ArrayList<ForeignKeyInfo> foreigns =
					getSorted(makeMaster(tableName), (idx, ds) -> new ForeignKeyInfo(ds));
			
			getAdditional(foreigns, makeSubKey(makeMaster(tableName)), (idx, ds, input) -> {
				List<String> pair = split(ds);
				input.cols1.add(pair.get(0));
				input.cols2.add(pair.get(1));
				return input;
			});
			
			cache = foreigns;
			cacheForeigns.set(tableName, cache);
		}
		
		return cache;
	}
	
	public void putForeignInfo(String tableName, ArrayList<ForeignKeyInfo> foreign)
	{
		ArrayList<String> mNames = new ArrayList<String>();
		for (int i=0; i<foreign.size(); i++)
		{
			String idxs;
			ForeignKeyInfo item = foreign.get(i);
			
			// check mastername is already exist in foreign names
			int mIdx = mNames.indexOf(item.tableName);
			if (mIdx != -1)
			{
				idxs = Integer.toString(mIdx);
			} else {
				idxs = Integer.toString(mNames.size());
				device.put(makeMaster(tableName), make(idxs, item.tableName));
				mNames.add(item.tableName);
			}
			
			for (int j=0; j<item.cols1.size(); j++)
			{
				device.put(make(makeMaster(tableName), idxs),
						make(item.cols1.get(j), item.cols2.get(j)));
			}
		}
		
		// make slave info
		for (String mName : mNames)
		{
			device.put(makeSlave(mName), tableName);
		}
	}
	
	public void deleteTable(String tableName) throws MyError
	{
		// delete slave info
		ArrayList<String> possible_masters = getTables();
		for (String possible_master : possible_masters)
		{
			device.delete(makeSlave(possible_master), (key, data) -> {
				return data.equals(tableName);
			});
		}
		
		device.deleteSub(makeSubKey(tableName), null);
		device.delete(makeTables(), (ks, ds) -> (ds.equals(tableName)) );
		
	}
	
	public static RowInfo parseRow (String data)
	{
		return new RowInfo(DBProcessor.map(split(data), (row) -> (new CellInfo(row, 0))));
	}
	
	public static String composeRow (RowInfo row)
	{
		return make(DBProcessor.map(row.cells, (cell) -> (cell.encode()) ) );
	}
	
	public ArrayList<RowInfo> selectRows(String tableName, RowLambda sel) throws MyError
	{
		return DBProcessor.map(
				device.get(
						makeRow(tableName), (post, data) -> (sel.f(parseRow(data)))
				), (data) -> (parseRow(data))
		);
	}
	
	public void insertRow(String tableName, RowInfo row)
	{
		device.put(makeRow(tableName), composeRow(row));
	}
	
	public void deleteRows(String tableName, RowLambda del) throws MyError
	{
		device.delete(makeRow(tableName), (key, data) -> {
			return del.f(parseRow(data));
		});
	}
	
}