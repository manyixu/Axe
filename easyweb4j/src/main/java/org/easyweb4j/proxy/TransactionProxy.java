package org.easyweb4j.proxy;

import java.lang.reflect.Method;

import org.easyweb4j.annotation.Aspect;
import org.easyweb4j.annotation.Service;
import org.easyweb4j.annotation.Tns;
import org.easyweb4j.helper.DataBaseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事务代理
 * Created by CaiDongYu on 2016/4/19.
 */
@Aspect(Service.class)
public class TransactionProxy extends AspectProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionProxy.class);

    private static final ThreadLocal<Boolean> FLAG_HOLDER = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    public Object doProxy(ProxyChain proxyChain) throws Throwable {
        Object result;
        boolean flag = FLAG_HOLDER.get();
        Method method = proxyChain.getTargetMethod();
        if(!flag && method.isAnnotationPresent(Tns.class)){
            FLAG_HOLDER.set(true);
            try {
                DataBaseHelper.beginTransaction();
                LOGGER.debug("begin transaction by TNS");
                result = proxyChain.doProxyChain();
                DataBaseHelper.commitTransaction();
                LOGGER.debug("commit transaction by TNS");
            } catch (Exception e){
                DataBaseHelper.rollbackTransaction();
                LOGGER.debug("rollback transaction",e);
                throw e;
            } finally {
                FLAG_HOLDER.remove();
            }
        } else {
            result = proxyChain.doProxyChain();
        }
        return result;
    }
}
