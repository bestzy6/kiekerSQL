package kieker.monitoring.probe.spring.executions;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * @author bestzy
 * @date 2022年3月22日 14点12分
 */
public class SqlUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SqlUtil.class);
    private SqlSessionFactory sqlSessionFactoryBean;

    //解析sql信息
    public String parseMybatisPersistentMethodSqlInfo(final MethodInvocation invocation) {
        String sqlId = "NULL";
        try {
            //id由classname和methodName组成
            String id = getClassName(invocation) + "." + invocation.getMethod().getName();
            Configuration conf = sqlSessionFactoryBean.getConfiguration();
            Object parameter = invocation.getArguments() != null && invocation.getArguments().length > 0 ? invocation.getArguments()[0] : null;
            //根据id获取相对应的MappedStatement,由此获得含有占位符的sql
            BoundSql boundSql = conf.getMappedStatement(id).getSqlSource().getBoundSql(parameter);
            sqlId = showCompletedSQL(conf, boundSql);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return sqlId;
    }

    //获取完整的SQL语句
    private String showCompletedSQL(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        //消除不必要的空格和换行符
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        //将占位符换成参数
        if (parameterMappings.size() > 0 && parameterObject != null) {
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", getParameterValue(parameterObject));
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    String[] s = metaObject.getObjectWrapper().getGetterNames();
                    s.toString();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", getParameterValue(obj));
                    }
                }
            }
        }
        return sql;
    }

    //为string类型参数加上单引号，将时间类型参数设置好。
    private String getParameterValue(Object obj) {
        String value;
        if (obj instanceof String) {
            value = "'" + obj + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }

    //获取类名
    public String getClassName(MethodInvocation invocation) {
        try {
            Proxy proxy = (Proxy) getReflectField(invocation, "proxy");
            InvocationHandler handler = Proxy.getInvocationHandler(proxy);
            ProxyFactory factory = (ProxyFactory) getReflectField(handler, "advised");
            Class[] classes = factory.getProxiedInterfaces();
            return classes[0].getName();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private Object getReflectField(Object obj, String fieldName) throws Exception {
        //获取类本身的属性成员（包括私有、共有、保护）
        Field field = obj.getClass().getDeclaredField(fieldName);
        //所以通过setAccessible(true)的方式关闭安全检查就可以达到提升反射速度的目的
        field.setAccessible(true);
        return field.get(obj);
    }

    //setter
    public void setSqlSessionFactoryBean(SqlSessionFactory sqlSessionFactoryBean) {
        this.sqlSessionFactoryBean = sqlSessionFactoryBean;
    }
}
