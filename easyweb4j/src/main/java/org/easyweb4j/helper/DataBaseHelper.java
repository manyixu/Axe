package org.easyweb4j.helper;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.easyweb4j.util.CollectionUtil;
import org.easyweb4j.util.ReflectionUtil;
import org.easyweb4j.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库 助手类
 * Created by CaiDongYu on 2016/4/15.
 */
public class DataBaseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataBaseHelper.class);

    private static final String DRIVER;
    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    private static final ThreadLocal<Connection> CONNECTION_HOLDER;

    private static final QueryRunner QUERY_RUNNER;

    private static final BasicDataSource DATA_SOURCE;

    static {

        //初始化jdbc配置
        DRIVER = ConfigHelper.getJdbcDriver();
        URL = ConfigHelper.getJdbcUrl();
        USERNAME = ConfigHelper.getJdbcUsername();
        PASSWORD = ConfigHelper.getJdbcPassword();
        //数据库连接池
        CONNECTION_HOLDER = new ThreadLocal<>();
        QUERY_RUNNER = new QueryRunner();
        DATA_SOURCE = new BasicDataSource();

        try {
            DATA_SOURCE.setDriverClassName(DRIVER);
            DATA_SOURCE.setUrl(URL);
            DATA_SOURCE.setUsername(USERNAME);
            DATA_SOURCE.setPassword(PASSWORD);
            
            //TODO:启动时同步表结构
        } catch (Exception e) {
            LOGGER.error("jdbc driver : " + DRIVER);
            LOGGER.error("jdbc url : " + URL);
            LOGGER.error("jdbc username : " + USERNAME);
            LOGGER.error("jdbc password : " + PASSWORD);
            LOGGER.error("load jdbc driver failure", e);
        }

    }

    /**
     * 获取数据库并链接
     */
    public static Connection getConnection() {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn == null) {
            try {
                conn = DATA_SOURCE.getConnection();
            } catch (SQLException e) {
                LOGGER.error("get jdbc connection failure", e);
                throw new RuntimeException(e);
            } finally {
                CONNECTION_HOLDER.set(conn);
            }
        }
        return conn;
    }

    /**
     * 关闭链接
     */
    public static void closeConnection() {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.error("close jdbc connection failure", e);
                throw new RuntimeException(e);
            } finally {
                CONNECTION_HOLDER.remove();
            }
        }
    }

    /**
     * 查询实体列表
     */
	public static <T> List<T> queryEntityList(final Class<T> entityClass, String sql, Object... params) {
        List<T> entityList;
        Connection conn = getConnection();
        try {
        	//BeanListHandler 不支持Date，所以自己实现
            //entityList = QUERY_RUNNER.query(conn, sql, new BeanListHandler<>(entityClass), params);
        	entityList = QUERY_RUNNER.query(conn, sql, new ResultSetHandler<List<T>>(){
        		@Override
        		public List<T> handle(ResultSet table) throws SQLException {
        			Method[] methods = entityClass.getDeclaredMethods();
					List<Method> setMethods = new ArrayList<>();
					for(Method method:methods){
						if(!method.getName().startsWith("set")) continue;
						
						setMethods.add(method);
					}
					List<T> list = new ArrayList<>();
        			while(table.next()){
						T obj = ReflectionUtil.newInstance(entityClass);
						for(Method setMethod:setMethods){
							String fieldName = setMethod.getName().substring(3);
							String columnName = StringUtil.camelToUnderline(fieldName);
							Object setMethodArg = table.getObject(columnName);
							ReflectionUtil.invokeMethod(obj, setMethod, setMethodArg);
						}
						list.add(obj);
					}
					return list;
        		}
        	},params);
        } catch (SQLException e) {
            LOGGER.error("query entity list failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return entityList;
    }

    /**
     * 查询单个实体
     */
    public static <T> T queryEntity(final Class<T> entityClass, String sql, Object... params) {
        T entity;
        Connection conn = getConnection();
        try {
        	//BeanHandler 不支持Date，所以自己实现
        	//entity = QUERY_RUNNER.query(conn, sql, new BeanHandler<>(entityClass), params);
        	entity = QUERY_RUNNER.query(conn, sql, new ResultSetHandler<T>(){

				@Override
				public T handle(ResultSet table) throws SQLException {
					if(table.next()){
						Method[] methods = entityClass.getDeclaredMethods();
						List<Method> setMethods = new ArrayList<>();
						for(Method method:methods){
							if(!method.getName().startsWith("set")) continue;
							
							setMethods.add(method);
						}
						T obj = ReflectionUtil.newInstance(entityClass);
						for(Method setMethod:setMethods){
							String fieldName = setMethod.getName().substring(3);
							String columnName = StringUtil.camelToUnderline(fieldName);
							Object setMethodArg = table.getObject(columnName);
							ReflectionUtil.invokeMethod(obj, setMethod, setMethodArg);
						}
						return obj;
					}
					return null;
				}
        	}, params);
        } catch (SQLException e) {
            LOGGER.error("query entity failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return entity;
    }

    /**
     * 执行List查询
     */
    public static List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = getConnection();
        try {
            result = QUERY_RUNNER.query(conn, sql, new MapListHandler(), params);
        } catch (Exception e) {
            LOGGER.error("execute queryList failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
    
    /**
     * 执行单条查询
     */
    public static Map<String, Object> queryMap(String sql, Object... params) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = getConnection();
        try {
            result = QUERY_RUNNER.query(conn, sql, new MapHandler(), params);
        } catch (Exception e) {
            LOGGER.error("execute queryMap failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
    

    /**
     * 执行返回结果是基本类型的查询
     */
    public static <T> T queryPrimitive(String sql, Object... params) {
    	T result = null;
        Connection conn = getConnection();
        try {
            result = QUERY_RUNNER.query(conn, sql, new ScalarHandler<T>(), params);
        } catch (Exception e) {
            LOGGER.error("execute queryMap failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
    


    /**
     * 执行更新语句 （包括 update、insert、delete）
     */
    public static int executeUpdate(String sql, Object... params) {
        int rows = 0;
        Connection conn = getConnection();
        try {
            rows = QUERY_RUNNER.update(conn, sql, params);
        } catch (SQLException e) {
            LOGGER.error("execute update failure", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if(conn.getAutoCommit()){
                    closeConnection();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return rows;
    }

    /**
     * 插入实体
     */
    public static boolean insertEntity(Class<?> entityClass, Map<String, Object> fieldMap) {
        if (CollectionUtil.isEmpty(fieldMap)) {
            LOGGER.error("can not insert entity: fieldMap is empty");
            return false;
        }

        String sql = "INSERT INTO " + getTableName(entityClass);
        StringBuilder columns = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (String fieldName : fieldMap.keySet()) {
            columns.append(fieldName).append(", ");
            values.append("?, ");
        }
        columns.replace(columns.lastIndexOf(", "), columns.length(), ")");
        values.replace(values.lastIndexOf(", "), values.length(), ")");
        sql += columns + " VALUES " + values;

        Object[] params = fieldMap.values().toArray();
        return executeUpdate(sql, params) == 1;
    }

    /**
     * 更新实体
     * TODO:这个还是只支持id是long类型的
     */
    public static boolean updateEntity(Class<?> entityClass, long id, Map<String, Object> fieldMap) {
        if (CollectionUtil.isEmpty(fieldMap)) {
            LOGGER.error("can not update entity: fieldMap is empty");
            return false;
        }

        String sql = "UPDATE " + getTableName(entityClass) + " SET ";
        StringBuilder columns = new StringBuilder();
        for (String fieldName : fieldMap.keySet()) {
            columns.append(fieldName).append("=?, ");
        }
        sql += columns.substring(0, columns.lastIndexOf(", ")) + " WHERE id=?";
        List<Object> paramList = new ArrayList<>();
        paramList.addAll(fieldMap.values());
        paramList.add(id);
        Object[] params = paramList.toArray();

        return executeUpdate(sql, params) == 1;
    }

    /**
     * 删除实体
     */
    public static boolean deleteEntity(Class<?> entityClass, long id) {
        String sql = "DELETE FROM " + getTableName(entityClass) + " WHERE id = ?";
        return executeUpdate(sql, id) == 1;
    }

    /**
     * TODO:这里没支持注解来指定表名，只是简单的根据类名来，这样对修改表名有障碍
     */
    private static String getTableName(Class<?> entityClass) {
        return entityClass.getSimpleName();
    }

    /**
     * 开启事务
     */
    public static void beginTransaction(){
        Connection conn = getConnection();
        if(conn != null){
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e){
                LOGGER.error("begin transaction failure",e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 提交事务
     */
    public static void commitTransaction(){
        Connection conn = getConnection();
        if(conn != null){
            try {
                conn.commit();
            } catch (SQLException e){
                LOGGER.error("commit transaction failure",e);
                throw new RuntimeException(e);
            }finally {
                try {
                    if(!conn.getAutoCommit()){
                        closeConnection();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 回滚事务
     */
    public static void rollbackTransaction(){
        Connection conn = getConnection();
        if (conn != null){
            try {
                conn.rollback();
            } catch (SQLException e){
                LOGGER.error("rollback transaction failure",e);
                throw new RuntimeException(e);
            } finally {
                closeConnection();
            }
        }
    }
}