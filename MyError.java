
public class MyError extends Throwable {
	public int type;

	public String arg;

	public static final int OK = 0;

	public static final int DuplicateColumnDefError = 1;

	public static final int DuplicatePrimaryKeyDefError = 2;

	public static final int ReferenceTypeError = 3;

	public static final int ReferenceNonPrimaryKeyError = 4;

	public static final int ReferenceColumnExistenceError = 5;

	public static final int ReferenceTableExistenceError = 6;

	public static final int NonExistingColumnDefError = 7;

	public static final int TableExistenceError = 8;

	public static final int CharLengthError = 9;

	public static final int NoSuchTable = 10;

	public static final int DropReferencedTableError = 11;

	public static final int ShowTablesNoTable = 12;

	public static final int InsertTypeMismatchError = 13;

	public static final int InsertColumnNonNullableError = 14;

	public static final int InsertColumnExistenceError = 15;

	public static final int InsertDuplicatePrimaryKeyError = 16;

	public static final int InsertReferentialIntegrityError = 17;

	public static final int WhereIncomparableError = 18;

	public static final int WhereTableNotSpecified = 19;

	public static final int WhereColumnNotExist = 20;

	public static final int WhereAmbiguousReference = 21;

	public static final int SelectTableExistenceError = 22;

	public static final int SelectColumnResolveError = 23;
	
	public static final int NoColumnInTableError = 81;
	
	public static final int DuplicatedItemInPrimaryKeyError = 82;
	
	public static final int ReferenceMultipleExistenceError = 83;
	
	public static final int InsertDuplicatedColNameError = 84;
	
	public static final int Control = 98;
	
	public static final int UnknownError = 99;
	

	public MyError(int et) {
		type = et;
		if (type == UnknownError)
			printStackTrace();
	}

	public MyError(int et, String a) {
		type = et;
		arg = a;
	}

	@Override
	public String toString()
	{
		switch (type)
		{
			case MyError.DuplicateColumnDefError : 
				return "Create table has failed: column definition is duplicated";
			case MyError.DuplicatePrimaryKeyDefError : 
				return "Create table has failed: primary key definition is duplicated";
			case MyError.ReferenceTypeError : 
				return "Create table has failed: foreign key references wrong type";
			case MyError.ReferenceNonPrimaryKeyError : 
				return "Create table has failed: foreign key references non primary key column";
			case MyError.ReferenceColumnExistenceError : 
				return "Create table has failed: foreign key references non existing column";
			case MyError.ReferenceTableExistenceError : 
				return "Create table has failed: foreign key references non existing table";
			case MyError.NonExistingColumnDefError : 
				return "Create table has failed: \'" + arg + "\' does not exists in column definition";
			case MyError.TableExistenceError : 
				return "Create table has failed: table with the same name already exists";
			case MyError.CharLengthError : 
				return "Char length should be over 0";
			case MyError.NoSuchTable : 
				return "No such table";
			case MyError.DropReferencedTableError : 
				return "Drop table has failed: \'" + arg + "\' is referenced by other table";
			case MyError.ShowTablesNoTable : 
				return "There is no table";
			case MyError.InsertTypeMismatchError:
				return "Insertion has failed: Types are not matched";
			case MyError.InsertColumnNonNullableError:
				return "Insertion has failed: \'" + arg + "\' is not nullable";
			case MyError.InsertColumnExistenceError:
				return "Insertion has failed: \'" + arg + "\' does not exist";
			case MyError.InsertDuplicatePrimaryKeyError:
				return "Insertion has failed: Primary key duplication";
			case MyError.InsertReferentialIntegrityError:
				return "Insertion has failed: Referential integrity violation";
			case MyError.WhereIncomparableError:
				return "Where clause try to compare incomparable values";
			case MyError.WhereTableNotSpecified:
				return "Where clause try to reference tables which are not specified";
			case MyError.WhereColumnNotExist:
				return "Where clause try to reference non existing column";
			case MyError.WhereAmbiguousReference:
				return "Where clause contains ambiguous reference";
			case MyError.SelectTableExistenceError:
				return "Selection has failed: \'" + arg + "\' does not exist";
			case MyError.SelectColumnResolveError:
				return "Selection has failed: fail to resolve \'" + arg + "\'";
			case MyError.NoColumnInTableError:
				return "There is no column in table";
			case MyError.DuplicatedItemInPrimaryKeyError:
				return "Create table has failed: Duplicated item in Primary Key";
			case MyError.ReferenceMultipleExistenceError:
				return "Create table has failed: foreign key references multiple column";
			case MyError.InsertDuplicatedColNameError:
				return "Insertion has failed: Column name duplication";
			default : 
				return "Unknown Error";
		}
	}
}