

# Database Project 1-3 Report

> 2016-12817 이승우

# 핵심 모듈과 알고리즘, 구현 내용

본 구현은 총 5개의 계층(layer)로 구성되어 있다. 상위 계층은 하위 계층을 이용하여 구현된다. 가장 하위에 Berkeley DB가 있고, 그 위에 DBDevice, 그 위에 DBInterface, 그 위에 DBProcessor, 맨 위에 SimpleDBMSParser 클래스가 있다. 그 중에서도 가장 핵심적인 처리를 수행하는 계층은 DBProcessor이다.

구현하면서 Java 8에 추가된 람다식을 적극적으로 활용하였다. 그러므로, 본 구현은 그 이상의 버전에서 컴파일되어야 한다.

## Berkeley DB

Berkeley DB는 라이브러리가 제공하는 (key, value) entry의 스토리지이다. setSortedDuplicates 옵션 때문에 내용이 중복될 수 있다. 본 구현은 내부에서 커서를 통해 Berkeley DB를 읽고 쓰는 방식으로 구현되므로, 계층구조의 가장 아랫단에 있다고 할 수 있다.

사용한 주요 함수들은 다음과 같다.
* open() : 커서를 연다.
* dup() : 커서를 복제한다.
* close() : 커서를 닫는다.
* putNoDupData() : 커서로 entry를 삽입한다.
* getSearchKey() : key에 해당하는 entry를 찾는다.
* getNextDup() : key가 같은 다음 entry를 찾는다.
* getSearchKeyRange() : key가 Greater than and equal한 entry를 찾는다. 
* getNext() : 다음 entry를 찾는다.
* delete() : entry를 지운다.
* sync() : 데이터베이스를 파일과 동기화한다.

[참고한 페이지](https://docs.oracle.com/cd/E17277_02/html/java/com/sleepycat/je/Cursor.html)

## DBDevice

DBDevice는 Berkeley DB를 편리하게 사용할 수 있도록 포장해 놓은 것으로, 어떤 특정한 하드웨어 디바이스를 모델링한다. 이 하드웨어 디바이스는 최대 두 개의 커서와 람다식을 활용해 (key, value)를 읽고 쓸 수 있다.

구현은 `DBDevice.java`에 존재한다.

인터페이스 함수들은 다음과 같다.
* 커서 관리 함수
  * 기본 커서 관리 : 가장 기본적으로 여는 커서이다. 쿼리 처리 시 DB 접근의 처음과 끝에 호출한다.
    * openCursor() : 디폴트 커서를 연다.
    * closeCursor() : 디폴트 커서를 닫는다.
  * 복제 커서 관리 : c가 열려 있는 중에 추가로 열 수 있는 커서이다. 이것이 열려 있는 동안의 데이터 조작 함수는 복제 커서를 사용한다. 람다 안에서 또다른 db 함수를 호출해야 하는 경우가 필요해 만들었다. 최대 하나만 가능하다.
    * openDupCursor() : 중첩 커서를 연다.
    * closeDupCursor() : 중첩 커서를 닫는다.
* 데이터 조작 함수 : key, data는 String, lambda는 (key, data) -> boolean이다.
  * put(key, data) : (key, data) entry를 만들어 넣는다.
  * get(key, lambda) : key가 정확히 일치하는 엔트리들 중 lambda를 만족시키는 것의 리스트를 구한다.
  * getSub(key, lambda) : key로 시작하는 엔트리들 중 lambda를 만족시키는 것의 리스트를 구한다.
  * delete(key, lambda) : key가 정확히 일치하는 엔트리들 중 중 lambda를 만족시키는 것들을 지운다.
  * deleteSub(key, lambda) : key로 시작하는 엔트리들 중 lambda를 만족시키는 것들을 지운다.

## DBInterface

DBInterface는 DBDevice 위에 table, column, row 등의 데이터베이스 개체들을 불러오고 저장하는 인터페이스를 구현한다. 또한, 성능 향상을 위해 간단한 Cache 역시 구현하였지만 과제 제출 가능 시간 내에 정상 동작하게 하기가 빠듯하여 사용하지는 않았다.

구현은 `DBInterface.java`에 존재한다.


* make~로 시작하는 함수 등 : 아래 서술된 path convention에 따라 path mangling을 수행한다.
* getSorted() : data에 idx/realdata 와 같은 식으로 표현된 entry들을 idx 기준으로 정렬된 ArrayList로 얻을 수 있도록 하는 Helper function.
, getAdditional() : key 끝에 index가 있는 entry들에 포함된 부가 정보를 쉽게 얻어올 수 있도록 하는 Helper function.
* 나머지 get, put, delete 로 시작하는 함수 등 : 위의 함수들을 사용하여, 데이터베이스 개체들을 path convention을 따라 알맞은 방식으로 읽고 쓴다.
* insertRow(), selectRows(), deleteRows() : 지정한 테이블에서 (람다식에 따라) 특정 Row(들)을 조작한다.
* 커서 관리 함수 : DBDevice의 것과 같은 역할을 한다.

### Entry Path Convention

버클리 DB는 (key, value) 쌍을 저장한다. 그러므로 Database 개체들의 구조를 entry 순서쌍들로 표현할 수 있어야 한다. 이에 본 구현에서는 directory 구조를 참고하여 다음과 같은 path convention을 사용한다. 식별자 구분을 위해 escape character로 식별자명에 사용하지 않는 특수문자인 '/'를 이용하였다.

* 전체 table들의 목록은 key '/table'를 가지는 entry들의 data들이다.
* table에 있는 column 정보는 key 'tablename/column'을 가지는 entry들이다. data에 columnidx/columnname 과 같은 식으로 기입한다. columnidx는 table 내에서 column별로 고유한 정수 값이다.
  * column들의 type에 대한 정보는 key 'tablename/type/columnidx'를 가지는 한 entry의 data에 정수 하나로 기입한다. 이 정수는 char이면 그 길이만큼이 되고, -2이면 int, -3이면 date 타입이 된다.
  * not null에 대한 정보는 key 'tablename/notnull/columnidx'를 가지는 한 entry의 data에 'Y' 또는 'N'으로 저장한다.
* primary key에 대한 정보는 key 'tablename/primary'를 가지는 entry들의 data들에 primary key를 구성하는 column 이름을 String으로 저장한다.
* foreign key에 대한 정보는 3가지 키에 나누어 저장한다. 혼동되지 않는 구분을 위해, 참조하는 테이블은 slave, 참조되는 테이블을 master로 표현하였다.
  * 이 테이블에 종속된 테이블들의 정보는 'tablename/slave'의 entry들의 data이다. 이 관계에서는 내가 master이다.
  * 이 테이블이 종속하는 테이블들의 정보는 key 'tablename/master'를 가지는 entry의 data에 masteridx/mastername 과 같은 식으로 기입한다. 이 관계에서는 내가 slave이고 master의 키를 참조한다.
    * 이 테이블에서 foreign key인 컬럼들의 이름은 key 'tablename/master/masteridx'를 가지는 entry의 data에 slavecolname/mastercolname 과 같은 식으로 기입한다.
* table의 row에 대한 정보는 key 'tablename/row'를 가지는 entry들의 data에 'cell0/cell1/cell2/...' 와 같이 relation row의 각 attribute가 담긴 cell을 '/' 구분자가 들어가는 방식으로 기입한다.
  * '/'문자가 들어가는 attribute의 데이터도 있기 때문에, type에 따라 cell을 다음과 같이 인코딩하였다.
    * null은 '%0', 숫자는 '%1' 을 따라오는 숫자 하나, string은 '%2'를 따라오는 문자열, date는 '%3'를 따라오는 NNNN-NN-NN 꼴로 저장된다.
    * 단, string에 포함된 '/' 문자 하나는 '%4'으로, '%'문자 하나는 '%5'으로 저장한다.

## DBProcessor

DBProcessor는 DBInterface를 사용하여 실제 Query의 error check와 그 처리를 구현하는 계층이다. 가장 핵심적인 처리를 수행한다.

구현은 `DBProcessor.java`에 존재한다.

함수들은 다음과 같다.

* 주요 유틸리티 함수들
  * debug(), debugList() : 디버깅용 함수들.
  * findInList(), findInListCheckDup() : 리스트에서 람다식을 만족시키는 원소의 인덱스를 반환한다. findInListCheckDup()이 붙은 것은 그런 원소가 중복되는지도 체크한다.
  * dual_for() : 람다식을 이용하여 두 개의 ArrayList를 동시에 순회한다. 크기가 맞지 않을 경우 지정된 에러를 던진다.
  * prettyPrint() : select Query에서 사용하여 결과를 깔끔하게 출력한다.
  * concat() : ProductRow의 리스트에 RowInfo를 붙인다.
  * resolveTable() : 테이블 이름들과 컬럼들, 별명 정보 등을 모두 받아, where절 등에서 참조하는 column 이름들이 어떤 테이블의 어떤 컬럼을 가리키는지 그 인덱스를 찾는다. 찾을 수 없다면 적절한 에러를 던진다.
* check~Query() : 각 쿼리들이 오류가 있는지 체크하고, 그 과정에서 쿼리를 실제로 처리하는 데 필요한 데이터를 모아 반환한다. 여기서는 entry의 변동이 일어나지 않는다.
* process ~ Query() : 대응되는 check ~ Query() 함수들을 부르고 오류가 없으면 그에서 얻은 정보로 실제로 쿼리를 처리한다. entry의 변동은 여기서 일어난다.
* processQuery() 세미콜론(;)으로 구분된 쿼리 목록을 받아 각 process 함수들을 호출해 쿼리를 하나씩 처리한다. 에러를 잡으면 에러 정의 파일인 `MyError.java`에 정의된 적절한 메시지를 출력한다.

### Variable Naming Convention

변수가 너무 많아 혼동되어, 대략 다음과 같은 variable naming convention을 적용하였다. 꼭 들어맞는 것은 아니며 상황에 따라 유연하게 사용한다.

* 주요 접두어
  * q : query의, query가 ~할
  * (없음) / t : 기존에 있던 table (column)의 ~
  * m : Foreign Key 관계에서 master 쪽(참조되는 쪽)의 ~ 
  * s : Foreign Key 관계에서 slave 쪽(참조하는 쪽)의 ~ 
  * f : Foreign key의 ~
  * Prm : 테이블의 primary key에 관한 ~
* 주요 접요어
  * Name : String인 이름
  * Col : 컬럼 하나를 표현하는 ColumnInfo 구조체
  * ColName : String인 컬럼의 이름
  * Val : attribute 하나를 표현하는 CellInfo 구조체
* 주요 접미어
  * (없음) : 그것 하나
  * s 또는 l : ArrayList로 표현되는 리스트
  * Exist : 존재하는지
  * Idx  (+ 대문자 접두어) : 대문자 접두어로 표현된 구조에서의 인덱스. 테이블 말고 컬럼의 인덱스도 뜻할 수 있는데 종종 Col 접요어를 생략한다.

예를 들면, selIdxTC는 TableColumn 구조체 배열에서의 select하는 column의 인덱스를 나타낸다. 또, sfIdxTs는 어떤 table의 slave table에 있는 어떤 foreign key info가 지정하는 column들의 원래 table(master 쪽)에서의 column 인덱스의 배열이 된다. 

아래는 InsertQuery, SelectQuery, DeleteQuery 처리의 세부사항이다.

### InsertQuery

InsertQuery의 처리과정은 다음과 같다. 먼저 checkInsertQuery의 처리과정을 보자.
* 먼저, 컬럼을 명시했을 경우 CreateTable 때 명시된 컬럼의 순서대로 컬럼과 값을 재정렬하고, 명시되지 않은 컬럼의 값은 일단 null 값으로 채운다.
* 컬럼을 명시하지 않았을 경우 CreateTable 때 명시된 컬럼의 순서대로 컬럼 이름을 넣어 준다. 
* 각 컬럼에 대한 람다식으로 `InsertTypeMismatchError`, `InsertColumnNonNullableError`과 같은 common constraint를 체크한다. string의 길이에 맞도록 truncation도 진행한다.
* 테이블의 각 foreign key 정보를 돌면서 foreign key constraint를 체크한다.
  * 그 foreign key에 포함된 slave 쪽 column들에 해당하는 value를 대응되는 master 쪽 column들의 순서에 따라 재정렬한 value의 리스트를 구한다. foreign key 관계로 연결되어 있지 않은 master 쪽 column에 해당되는 순서의 값은 비워 둔다.
  * master table의 열을 모두 돌면서, 앞에서 만든 value의 리스트인 Cell Set과 같은 master 쪽의 Cell set이 하나라도 있는지 검증한다. 없다면 `InsertReferentialIntegrityError`이다.
* 테이블의 row에 대한 람다식으로 primary constraint를 체크한다.
  * 각 primary column마다 row의 해당하는 cell이 insert하려는 value와 같은지 확인한 후, 모든 primary column에 대해 Cell 값이 같은 row가 있으면 `InsertDuplicatePrimaryKeyError`이다.

checkInsertQuery는 앞서 reordering 과정과 컬럼 이름, null 채우기 등을 진행한 Query를 반환한다. processInsert에서는 이 정보로부터 row를 만들어 insert하게 된다.

### SelectQuery

SelectQuery의 처리과정은 다음과 같다. entry 변경이 없으므로 checkSelectQuery가 거의 대부분을 담당한다.

* from 절의 테이블들이 존재하는지 확인한다.
* from 절에 명시된 각 테이블들에 대해, 테이블 이름과 각 테이블들의 컬럼들, 있다면 테이블의 별명까지 모은 TableColumns 구조체의 리스트를 만든다.
* wildcard로 지정된 모든 컬럼 select의 경우, 쿼리의 select list를 from list에 나열된 모든 테이블의 모든 컬럼들로 채워 준다.
* 추후 processSelectQuery에서 출력할 수 있도록, SelectInfo 구조체의 인덱스들(select list에 있는 어떤 항목은 TableColumn list의 어떤 테이블의 어떤 컬럼을 가리키는지)을 채운다.
* from 절의 각 테이블마다 반복하며 다음을 처리한다.
  * 이제까지의 테이블들만을 고려하여 선택된 row들은 productRows에 저장되어 있다.
  * 현재 table들의 row들을 모두 돌면서, 이번 테이블까지 고려하여 선택된 row들 (selectedProductRows)을 만들어야 한다.
    * where 절이 있다면 where.eval_check를 호출하여 이번 row를 productRows에 덧붙여 본 새로운 ProductRow가 확실히 포함되지 않는다면(FALSE/UNKNOWN) 버리고, 확실히 포함되거나 불확실하다면(TRUE/FUTURE) selectedProductRows에 포함한다.
  * 이렇게 선택된 ProductRows는 다음 productRows 변수에 저장한다.
* 마지막에는 모든 테이블을 고려하므로, select의 결과가 포함된 productRows가 결과로 나오게 된다. 이를 SelectInfo 구조체에 포함해 다른 정보들과 함께 반환한다.
* processSelectQuery에서는 SelectInfo 구조체의 정보들을 이용하여, 쿼리에 요청된 정보를 prettyPrint 함수를 이용하여 출력한다.

Select가 여러 테이블의 정보를 함께 다루다 보니, 여러 테이블의 Row의 cartesian product를 한 번에 표현하는 ProductRow 클래스를 만들었다. 이는 테이블 이름과 그에 해당하는 테이블의 RowInfo를 담고 있다.

#### where절 내부 처리

where절은 `BoolExprInfo` 클래스로 표현된다. 하나의 Comparison/Null predicate나 여러 Predicate의 OR 식, AND 식, NOT 식을 표현할 수 있으며, OR이나 AND는 short circuit을 수행한다. eval_check는 하위 BoolExprInfo의 eval_check나 predicate의 eval_check를 호출하는 식으로 구현되어 있다.
반환값이 boolean이 아니라 Bool3이라는 클래스인데, 이는 단순한 TRUE/FALSE 값뿐만 아니라 null 값과 비교했을 때 나오는 UNKNOWN이나 이제까지의 테이블만 보고는 값이 불확실함을 나타내는 FUTURE를 담을 수 있도록 하였다.
CompPredicate 클래스가 사용하는 Operand 클래스나 NullPredicate 클래스의 eval함수는 쿼리에서 넘겨준 정보들을 받아 resolveTable 함수를 사용하여 주어진 정보(테이블 이름, 컬럼 이름, 별명)를 활용하여 주어진 ProductRow로부터 어떤 테이블의 어떤 컬럼에 해당하는 값을 가져와야 하는지 살펴본 후, 아직 고려하지 말아야 할 테이블로부터 온 정보가 필요한 경우 FUTURE로 처리하고 아니면 적절히 비교하여 값을 내놓는다. 이렇게 Bool3 값이 트리 형식으로 계산되어 최종적으로 ProductRow 하나에 대한 Bool3 값을 내놓는다.

### DeleteQuery

Delete의 처리 과정은 다음과 같다.

* delete에서 고려하는 table은 하나밖에 없으며 별명도 없으므로, checkDeleteQuery에서는 where절을 검증하기 위해 간단히 delete 쿼리의 대상 table의 각 row에 대해 eval_check를 한 번씩 불러 준다.
* 이제 delete 취소를 고려하기 위해 먼저 getCancelHints를 호출하여 각 slave table에 대한 Cancellation Hints를 얻는다.
  * slave table 하나마다 DeleteCancelHints 클래스가, slave table의 각 foreign key 정보마다 DeleteCancelHint 클래스가 대응된다.
  * 각 DeleteCancelHint는 foreign key info의 slave 쪽 컬럼들의 slave table에서의 index들과, master 쪽 컬럼들의 원래 table에서의 index들을 포함하도록 한다. 추가로 고려되는 slave 쪽 컬럼들이 not null인지도 저장한다.
* processDelete 함수에서는 이 정보들을 이용하여 deleteRows 함수를 호출, 실제로 table의 row의 delete와 slave row의 update를 진행한다.
  * 대상 table의 각 row마다, eval_check를 호출하여 row 지우는 것을 시도할지를 확인한다.
  * 만약 row 지우는 것을 시도해야 한다면, 아래와 같은 과정으로 삭제 취소 조건과 slave table update를 확인한다.
    * 각 slave들에 대응되는 각 DeleteCancelHints에 대해 반복하면서, 해당 slave 테이블의 업데이트를 위해 deleteRows 함수를 호출한다. 이 때 Device를 접근하는 중에 다시 Device를 접근해야 하므로, 복제 커서를 이용하여야 한다. 
    * slave 테이블의 각 row에 대해, slave의 각 foreignKey 정보에 해당하는 각 DeleteCancelHint에 대해 반복하면서, 취소를 수행할지를 결정하기 위해 대상 table의 row와 slave table의 row의 Cell set을 비교한다. 만약 힌트에 있는 foreign key 관계의 cell들이 모두 같다면, 이 slave row가 업데이트되거나 삭제되어야 한다.
      * dch에 있는 notnull 정보를 이용하여, 고려되는 컬럼이 모두 nullable하다면 slave row가 업데이트되어야 한다. row에서 해당하는 Cell들이 null이 되도록 설정하고 이 row는 삭제 후 변화된 정보로 다시 update되도록 한다.
      * 고려되는 컬럼들 중 하나라도 not null이면, 이 원래 table의 row에 대한 삭제는 취소되어야 한다. 취소 카운트를 증가시키고 삭제를 취소하게 된다.
    * deleteRows가 끝나면 업데이트 대상이 되는 row들은 지워져 있는데, 변화된 정보를 가진 row를 다시 삽입한다.
  * 삭제가 확정된 row에 대해서는 삭제 카운트를 증가시키고 삭제한다. 

## SimpleDBMSParser

SimpleDBMSParser는 가장 위에 있는 계층이며, 사용자와 상호작용하면서 입력받은 쿼리를 파싱하여 DBProcessor에게 그 처리를 맡기는 계층이다.

구현은 `MyNewGrammer.jj`에 존재한다. 이제까지의 project들에서처럼, JavaCC 문법에 맞게 파일을 작성하였다. 상황에 따라 적절한 LOOKAHEAD를 지정한다.

# 그 외 내용

## 구현하지 못한 내용

필요한 사항은 모두 구현하였다고 생각한다. 다만, 테스트가 충분하지 않아 실제로는 제대로 동작하지 않는 부분이 있을 수도 있다.

## 가정한 것들

다음은 가능한 특이 케이스의 처리에서 가정한 것들이다.
* column name이 주어졌을 때는 nullable 을 제외한 column에 해당하는 값을 포함한다면 partial insert가 가능하다.
* type이 다르면 같은 null이라도 Incomparable 에러를 출력한다.
* logical operator는 short circuit이 가능해서, 값에 따라 에러가 나기도 하고 안 나기도 한다.
* `select id as S, S as T from student` 와 같이 별명 붙은 column이 다른 column의 별명을 참조하는 경우 에러를 출력한다.
* 단, `select S.id as T from student as S`와 같이 별명 붙은 column이 어떤 table의 별명을 참조하는 것은 가능하다.


다음과 같은 경우에 추가적인 에러를 출력한다.
* table에 아예 column들이 없을 때 
* primary key 하나에 중복되는 항목이 있을 때
* 같은 foreign key가 여러 테이블의 칼럼들을 참조할 때
* insert Query의 컬럼 이름 중 중복된 것이 있을 때

## 컴파일과 실행 방법

지난번에서 크게 달라지는 것은 없다. 단, 메인 폴더 안에 db 폴더를 미리 만들어 주어야 한다.

컴파일은 Eclipse 에서 자동적으로 진행되었고, JAR 파일 만들기는 [링크](https://veenvalu.tistory.com/1) 를
참고하였다.
`java -jar PRJ1-3_2016-12817.jar` 커맨드를 셸에 입력하면 실행이 된다.

## 프로젝트를 하면서 느낀 점

이번 프로젝트는 OS 프로젝트와 그 기간이 겹치는 바람에 매우 힘들었다. 더구나 Project 1-2가 제대로 동작하지 않아 상당 부분 1-1의 구현에서부터 다시 시작하다 보니 구현할 양이 엄청나게 많았다. 프로젝트의 총 코드 줄 수는 javaCC가 생성한 코드를 제외하고도 4000줄에 육박한다. 그러다 보니, 제 시간에 제출하지 못하고 delay를 많이 사용하였다.
또한, String 타입의 이름을 메인 데이터로 삼아 그 인덱스를 찾는 코드를 많이 넣다 보니 성능이 더 떨어지고 코드가 번잡해지는 문제점이 있었다. 이 프로젝트를 다시 하게 된다면 인덱스를 메인 데이터로 삼는 개선을 할 것이다.
너무나 구현해야 할 것이 많다 보니, 개인적으로는 차라리 충분히 많이 구현되어 있는 스켈레톤 코드를 주고 정밀한 에러 처리나 적절한 transaction의 동기화, 성능 향상 등을 구현하는 프로젝트를 하면 더 좋겠다고 생각했다. 딜레이로 제출하는데 많이 힘들었습니다ㅠㅠ
