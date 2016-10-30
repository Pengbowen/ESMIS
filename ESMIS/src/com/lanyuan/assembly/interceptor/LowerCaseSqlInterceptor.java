package com.lanyuan.assembly.interceptor;


import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * 将待执行SQL转换为全小写的拦截器
 *
 * @author dylan
 */
@Intercepts({@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
        , @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class LowerCaseSqlInterceptor implements Interceptor {
    private static Logger logger = Logger.getLogger(LowerCaseSqlInterceptor.class);

    public Object intercept(Invocation invocation) throws Throwable {
        String method = invocation.getMethod().getName();
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        SqlSource sqlSource = wrapperSqlSource(ms, ms.getSqlSource(), invocation.getArgs()[1], method);
        Field sqlSourceField = MappedStatement.class.getDeclaredField("sqlSource");
        ReflectionUtils.makeAccessible(sqlSourceField);
        ReflectionUtils.setField(sqlSourceField, ms, sqlSource);
        return invocation.proceed();
    }

    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    public void setProperties(Properties properties) {

    }

    private SqlSource wrapperSqlSource(MappedStatement ms, SqlSource sqlSource, Object parameter, String method){
        BoundSql originBoundSql = sqlSource.getBoundSql(parameter);
        String sql = originBoundSql.getSql();
//        logger.info("method type : {"+method+"}, source sql : { "+sql+" }");
        
        SqlSource wrapper = new SqlSourceWrapper(sqlSource);
        sql = wrapper.getBoundSql(parameter).getSql();
        logger.info("method type : {"+method+"}");
        logger.info("source sql : {"+sql+"}");
        logger.info("parameter : {"+JSON.toJSONString(parameter)+"}");
        return wrapper;
    }

    public static class SqlSourceWrapper implements SqlSource{
        private SqlSource origin ;
        public SqlSourceWrapper(SqlSource origin){
            this.origin = origin;
        }

        public BoundSql getBoundSql(Object parameterObject) {
            BoundSql boundSql = origin.getBoundSql(parameterObject);
            String sql = boundSql.getSql();
            Field sqlField = null;
            try {
                sqlField = BoundSql.class.getDeclaredField("sql");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            ReflectionUtils.makeAccessible(sqlField);
            ReflectionUtils.setField(sqlField, boundSql, sql.toLowerCase());
            return boundSql;
        }
    }

}
