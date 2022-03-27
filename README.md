# Kieker

kieker具体使用请参考

kieker地址：[https://github.com/kieker-monitoring/kieker](https://github.com/kieker-monitoring/kieker)

## 快速上手

在mybatis的Mapper接口中添加注解`kiekerSQL`，如

```java
@KiekerSql
public interface CategoryMapper {

  List<Category> getCategoryList();

  Category getCategory(String categoryId);

}
```

在spring配置中添加AOP，如

````xml
    <bean id="util" class="kieker.monitoring.probe.spring.executions.SqlUtil">
        <property name="sqlSessionFactoryBean" ref="sqlSessionFactory"/>
    </bean>
    <bean id="opEMIIWS"
          class="kieker.monitoring.probe.spring.executions.OperationExecutionMethodInvocationInterceptorWithSQL">
        <property name="sqlUtil" ref="util"/>
    </bean>
    <aop:config>
        <aop:advisor advice-ref="opEMIIWS" pointcut="execution(public * org.mybatis.jpetstore..*.*(..))"/>
    </aop:config>
````

## 监控结果展示

以 [jpetstore](https://github.com/mybatis/jpetstore-6) ，为例：

```
$0;1648365529819956900;1.15;KIEKER-SINGLETON;LAPTOP-QRJSTFAO;1;false;0;NANOSECONDS;0
$1;1648365534156809500;public abstract java.util.List org.mybatis.jpetstore.persistence.ProductMapper.getProductListByCategory(java.lang.String);<no-session-id>;5793458708147077121;1648365534083055800;1648365534156653200;method;1;1
$1;1648365534156809100;public abstract java.util.List org.mybatis.jpetstore.persistence.ProductMapper.getProductListByCategory(java.lang.String);<no-session-id>;5793458708147077122;1648365534083055800;1648365534156642000;method;1;1
$1;1648365534157044900;SELECT PRODUCTID, NAME, DESCN as description, CATEGORY as categoryId FROM PRODUCT WHERE CATEGORY = 'DOGS';<no-session-id>;5793458708147077121;1648365534157042800;1648365534157042800;sql;2;2
$1;1648365534157044900;SELECT PRODUCTID, NAME, DESCN as description, CATEGORY as categoryId FROM PRODUCT WHERE CATEGORY = 'DOGS';<no-session-id>;5793458708147077122;1648365534157042900;1648365534157042900;sql;2;2
$1;1648365534157053800;public java.util.List org.mybatis.jpetstore.service.CatalogService.getProductListByCategory(java.lang.String);<no-session-id>;5793458708147077122;1648365534071196600;1648365534157052800;method;0;0
$1;1648365534157054000;public java.util.List org.mybatis.jpetstore.service.CatalogService.getProductListByCategory(java.lang.String);<no-session-id>;5793458708147077121;1648365534071196500;1648365534157052800;method;0;0
```

