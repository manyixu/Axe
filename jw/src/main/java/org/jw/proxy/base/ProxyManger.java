package org.jw.proxy.base;

import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

/**
 * 代理管理器
 * 负责创建代理对象
 * Created by CaiDongYu on 2016/4/14.
 */
public class ProxyManger {

    @SuppressWarnings("unchecked")
	public static <T> T createProxy(final Class<T> targetClass, final List<Proxy> proxyList){
        return (T) Enhancer.create(targetClass, (MethodInterceptor) (targetObject, targetMethod, methodParams, methodProxy) ->
        new ProxyChain(targetClass,targetObject,targetMethod,methodProxy,methodParams,proxyList).doProxyChain()
        );
    }
}
