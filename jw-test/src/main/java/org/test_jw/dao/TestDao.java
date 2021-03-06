package org.test_jw.dao;

import java.util.List;

import org.jw.annotation.persistence.Dao;
import org.jw.annotation.persistence.Sql;
import org.jw.bean.persistence.Page;
import org.jw.bean.persistence.PageConfig;
import org.jw.interface_.persistence.BaseRepository;
import org.test_jw.bean.Export;
import org.test_jw.bean.just4test;

@Dao
public interface TestDao extends BaseRepository{

	@Sql("select * from just4test where id = ?")
	public just4test getOne(long id);

	@Sql("select * from just4test")
	public List<just4test> getAll();
	
	@Sql("select * from just4test where name like '%test%'")
	public Page<just4test> page();
	
	@Sql("select * from Export")
	public List<Export> getAllExport();
	
	@Sql("select * from Export where name like ?1")
	public Page<Export> pagingExport(String name,PageConfig pageConfig);
}
