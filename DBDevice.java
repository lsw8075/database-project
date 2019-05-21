import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

// models a hardware device by Berkeley DB
public class DBDevice {
	Database db;
	Cursor c, c_saved;

	@FunctionalInterface
	interface DeviceLambda {
		boolean apply(String key, String data) throws MyError;
	}

	public DBDevice(Database _db) {
		this.db = _db;
		c = null;
		c_saved = null;
	}

	public void openCursor() {
		if (c != null)
			c.close();
		c = db.openCursor(null, null);
		c_saved = null;
	}

	public void closeCursor() {
		if (c != null)
			c.close();
		c = null;
		// write data to disk
		db.sync();
	}

	public void openDupCursor() {
		if (c != null) {
			c_saved = c;
			c = c_saved.dup(false);
		}
	}

	public void closeDupCursor() {
		if (c_saved != null) {
			c.close();
			c = c_saved;
			c_saved = null;
		}
	}

	// most low-level functions

	public void put(String key, String value) {
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry(value.getBytes("UTF-8"));
			c.putNoDupData(keyEntry, dataEntry);
			DBProcessor.debug("put (" + key + ", " + value + ")");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void skip() {
		// move cursor to the last position
		DatabaseEntry keyEntry = new DatabaseEntry();
		DatabaseEntry dataEntry = new DatabaseEntry();
		c.getLast(keyEntry, dataEntry, LockMode.DEFAULT);
	}

	public ArrayList<String> get(String key, DeviceLambda l) throws MyError {
		ArrayList<String> result = new ArrayList<String>();
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKey(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				do {
					String dataString = new String(dataEntry.getData(), "UTF-8");
					DBProcessor.debug("get (" + key + ", " + dataString + ")");
					if (l == null || l.apply(key, dataString))
						result.add(dataString);
				} while (c.getNextDup(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	public ArrayList<String> getSub(String key, DeviceLambda l) throws MyError {
		ArrayList<String> result = new ArrayList<String>();
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKeyRange(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				do {
					String keyString = new String(keyEntry.getData(), "UTF-8");
					String dataString = new String(dataEntry.getData(), "UTF-8");

					if (keyString.startsWith(key)) {
						DBProcessor.debug("get (" + keyString + ", " + dataString + ")");
						if (l == null || l.apply(keyString, dataString))
							result.add(dataString);
					} else
						break;

				} while (c.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	/*
	public String getOne(String key) throws MyError {
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKey(keyEntry, dataEntry, LockMode.DEFAULT) != OperationStatus.SUCCESS)
				return null;
			return new String(dataEntry.getData(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	*/
	public void delete(String key, DeviceLambda l) throws MyError {
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKey(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				do {
					String dataString = new String(dataEntry.getData(), "UTF-8");
					if (l == null || l.apply(key, dataString))
					{
						DBProcessor.debug("delete (" + key + ", " + dataString + ")");
						c.delete();
					}
				} while (c.getNextDup(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void deleteSub(String key, DeviceLambda l) throws MyError {
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKeyRange(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				do {
					String keyString = new String(keyEntry.getData(), "UTF-8");
					String dataString = new String(dataEntry.getData(), "UTF-8");

					if (keyString.startsWith(key)) {
						if (l == null || l.apply(keyString, dataString))
						{
							DBProcessor.debug("delete (" + keyString + ", " + dataString + ")");
							c.delete();
						}
					} else
						break;

				} while (c.getNext(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	/*
	public void deleteOne(String key) throws MyError {
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataEntry = new DatabaseEntry();
			if (c.getSearchKey(keyEntry, dataEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS)
				c.delete();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	*/
}
