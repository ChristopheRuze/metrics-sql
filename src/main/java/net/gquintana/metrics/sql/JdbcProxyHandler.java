package net.gquintana.metrics.sql;

import com.codahale.metrics.Timer;
import net.gquintana.metrics.proxy.ProxyHandler;
import net.gquintana.metrics.proxy.MethodInvocation;
import net.gquintana.metrics.proxy.AbstractProxyFactory;
import java.sql.SQLException;
import java.sql.Wrapper;
import net.gquintana.metrics.proxy.ProxyClass;

/**
 * Base class for all JDBC Proxy factories.
 * 
 * @param <T> Proxied type
 */
public abstract class JdbcProxyHandler<T> extends ProxyHandler<T> {

    /**
     * JDBC Interface class
     */
    private final Class<T> delegateType;
    /**
     * Timer measuring this proxy lifetime
     */
    private final Timer.Context lifeTimerContext;
    /**
     * Proxy name
     */
    protected final String name;
    /**
     * Parent factory of proxy factories
     */
    protected final JdbcProxyFactory proxyFactory;

    /**
     * Main constructor
     *
     * @param delegate Wrapped JDBC object
     * @param delegateType JDBC object interface
     * @param name Proxy name
     * @param proxyFactory Parent factory
     * @param lifeTimerContext Proxy life split
     */
    protected JdbcProxyHandler(T delegate, Class<T> delegateType, String name, JdbcProxyFactory proxyFactory, Timer.Context lifeTimerContext) {
        super(delegate);
        this.delegateType = delegateType;
        this.proxyFactory = proxyFactory;
        this.name = name;
        this.lifeTimerContext = lifeTimerContext;
    }

    private boolean isDelegateType(Class<?> iface) {
        return this.delegateType.equals(iface);
    }

    private Class getClassArg(MethodInvocation methodInvocation) {
        return (Class) methodInvocation.getArgAt(0, Class.class);
    }

    protected Object isWrapperFor(MethodInvocation methodInvocation) throws Throwable {
        final Class iface = getClassArg(methodInvocation);
        return isDelegateType(iface) ? true : methodInvocation.proceed();
    }

    protected Object close(MethodInvocation methodInvocation) throws Throwable {
        stopTimer(lifeTimerContext);
        return methodInvocation.proceed();
    }

    protected final void stopTimer(Timer.Context split) {
        split.stop();
    }

    protected Object unwrap(MethodInvocation<T> methodInvocation) throws SQLException {
        final Class iface = getClassArg(methodInvocation);
        final Wrapper delegateWrapper = (Wrapper) delegate;
        Object result;
        if (isDelegateType(iface)) {
            result = delegateWrapper.isWrapperFor(iface) ? delegateWrapper.unwrap(iface) : iface.cast(delegateWrapper);
        } else {
            result = delegateWrapper.unwrap(iface);
        }
        return result;
    }

    public ProxyClass getProxyClass() {
        return new ProxyClass(delegate.getClass().getClassLoader(), delegateType);
    }
}