package kieker.monitoring.probe.spring.executions;

import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.registry.ControlFlowRegistry;
import kieker.monitoring.core.registry.SessionRegistry;
import kieker.monitoring.probe.IMonitoringProbe;
import kieker.monitoring.timer.ITimeSource;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * @author bestzy
 * @date 2022年3月25日
 */
public class OperationExecutionMethodInvocationInterceptorWithSQL implements MethodInterceptor, IMonitoringProbe {
    private static final Logger LOG = LoggerFactory.getLogger(OperationExecutionMethodInvocationInterceptorWithSQL.class);
    private static final SessionRegistry SESSION_REGISTRY = SessionRegistry.INSTANCE;
    private static final ControlFlowRegistry CF_REGISTRY = ControlFlowRegistry.INSTANCE;
    private final IMonitoringController monitoringCtrl;
    private final ITimeSource timeSource;
    private String OperationType;
    //类型
    private final String TYPE_METHOD = "method";
    private final String TYPE_SQL = "sql";
    //类注解
    private final String Annotation_MYBATIS = "KiekerSql";
    private SqlUtil sqlUtil;

    public OperationExecutionMethodInvocationInterceptorWithSQL() {
        this(MonitoringController.getInstance());
    }

    public OperationExecutionMethodInvocationInterceptorWithSQL(final IMonitoringController monitoringController) {
        this.monitoringCtrl = monitoringController;
        this.timeSource = this.monitoringCtrl.getTimeSource();
        //在构造方法中初始化
        this.OperationType = TYPE_METHOD;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        if (!this.monitoringCtrl.isMonitoringEnabled()) {
            return invocation.proceed();
        }
        //signature是方法名称
        String signature = invocation.getMethod().toString();
        if (!this.monitoringCtrl.isProbeActivated(signature)) {
            return invocation.proceed();
        }

        final String sessionId = SESSION_REGISTRY.recallThreadLocalSessionId();
        final int eoi; // this is executionOrderIndex-th execution in this trace
        final int ess; // this is the height in the dynamic call tree of this execution
        final boolean entrypoint;
        //此方法返回以前使用registerTraceId（CurtTraceID）方法注册的线程本地traceid。
        long traceId = CF_REGISTRY.recallThreadLocalTraceId(); // traceId, -1 if entry point
        if (traceId == -1) {
            //入口
            entrypoint = true;
            traceId = CF_REGISTRY.getAndStoreUniqueThreadLocalTraceId();
            CF_REGISTRY.storeThreadLocalEOI(0);
            CF_REGISTRY.storeThreadLocalESS(1); // next operation is ess + 1
            //执行顺序索引
            eoi = 0;
            //执行堆栈大小
            ess = 0;
        } else {
            entrypoint = false;
            eoi = CF_REGISTRY.incrementAndRecallThreadLocalEOI(); // ess > 1
            ess = CF_REGISTRY.recallAndIncrementThreadLocalESS(); // ess >= 0
            if ((eoi == -1) || (ess == -1)) {
                LOG.error("eoi and/or ess have invalid values:" + " eoi == " + eoi + " ess == " + ess);
                this.monitoringCtrl.terminateMonitoring();
            }
        }
        //判断是否为DAO类
        if (isPersistentClassMethod(invocation)) {
            String oldClassName = invocation.getMethod().getDeclaringClass().getCanonicalName();
//            LOG.warn("oldClassName:" + oldClassName);
            String newClassName = this.sqlUtil.getClassName(invocation);
//            LOG.warn("newClassName:" + oldClassName);
            signature = signature.replace(oldClassName, newClassName);
        }
        //
        final long tin = this.timeSource.getTime();
        final Object retval;
        try {
            //执行方法
            retval = invocation.proceed();
        } finally {
            final long tout = this.timeSource.getTime();
            this.monitoringCtrl.newMonitoringRecord(new OperationExecutionRecord(signature, sessionId, traceId, tin, tout, this.OperationType, eoi, ess));
            //写入sql日志，方法中会判断是否为DAO方法
            this.recordSQLInfo(invocation, ess + 1);
            // cleanup
            if (entrypoint) {
                //此方法取消设置以前注册的traceid。
                CF_REGISTRY.unsetThreadLocalTraceId();
                CF_REGISTRY.unsetThreadLocalEOI();
                CF_REGISTRY.unsetThreadLocalESS();
            } else {
                // next operation is ess
                CF_REGISTRY.storeThreadLocalESS(ess);
            }
        }
        return retval;
    }

    //记录sql语句
    private void recordSQLInfo(final MethodInvocation invocation, int parentIndex) {
        try {
            if (isPersistentClassMethod(invocation)) {
                String sqlID = sqlUtil.parseMybatisPersistentMethodSqlInfo(invocation);
                recordSQL(sqlID, parentIndex);
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    //将sql语句打印到日志
    private void recordSQL(final String sqlID, final int parentNodeIndex) {
        final String sessionId = SESSION_REGISTRY.recallThreadLocalSessionId();
        final int eoi; // this is executionOrderIndex-th execution in this trace
        final int ess; // this is the height in the dynamic call tree of this execution
        long traceId = CF_REGISTRY.recallThreadLocalTraceId(); // traceId, -1 if entry point
        //
        if (traceId == -1) {
            traceId = CF_REGISTRY.getAndStoreUniqueThreadLocalTraceId();
            CF_REGISTRY.storeThreadLocalEOI(0);
            CF_REGISTRY.storeThreadLocalESS(1);
            eoi = 0;
            ess = 0;
        } else {
            eoi = CF_REGISTRY.incrementAndRecallThreadLocalEOI();
            ess = parentNodeIndex;
            if ((eoi == -1) || (ess == -1)) {
                LOG.error("eoi and/or ess have invalid values:" + " eoi == " + eoi + " ess == " + ess);
                this.monitoringCtrl.terminateMonitoring();
            }
        }
        //并不记录sql的执行时间，所以日志中的时间一律为tin
        final long tin = this.timeSource.getTime();
        //
        this.monitoringCtrl.newMonitoringRecord(
                new OperationExecutionRecord(sqlID, sessionId, traceId, tin, tin, TYPE_SQL, eoi, ess));

    }

    //判断当前类是否为dao类,本质上是判断是否为mybatis
    private boolean isPersistentClassMethod(final MethodInvocation invocation) {
        Annotation[] annotations = invocation.getMethod().getDeclaringClass().getAnnotations();
        for (Annotation a : annotations) {
            if (Annotation_MYBATIS.equalsIgnoreCase(a.annotationType().getSimpleName())) {
                return true;
            }
        }
        //如果通过自定义注解判断失败
        return false;
    }

    //用spring注入
    public void setSqlUtil(SqlUtil sqlUtil) {
        this.sqlUtil = sqlUtil;
    }
}

