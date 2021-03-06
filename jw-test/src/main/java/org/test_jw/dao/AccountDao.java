package org.test_jw.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jw.annotation.persistence.Dao;
import org.jw.annotation.persistence.Sql;
import org.test_jw.bean.Account;

@Dao
public interface AccountDao {
	
	@Sql("select * from iot_user_account")
	public List<Account> getAll();

	@Sql("select * from iot_user_account limit 1")
	public Account getLimit1();
	
	@Sql("select count(*) 'all' from iot_user_account")
	public Map<String,Double> getCount();
	
	@Sql("select * from iot_user_account limit 1")
	public Map<String,Object> getMap();

	@Sql("select * from iot_user_account limit 1")
	public Map getMap2();
	

	@Sql("select regeist_date from iot_user_account where id=?")
	public List<Date> getDate(long id);
	

	@Sql("select mobile_validate from iot_user_account")
	public List<Integer> getIntegerList();

	@Sql("select id from iot_user_account")
	public List<Long> getLongList();
}
