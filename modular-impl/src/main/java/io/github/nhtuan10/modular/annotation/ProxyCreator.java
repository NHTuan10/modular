package io.github.nhtuan10.modular.annotation;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Slf4j
public class ProxyCreator {
    // TODO: need to handle multiple interfaces
    public static Object createNoArgsContructorsProxyClass(Class<?> interfaceClass, Class<?> impl) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Object implObject = impl.getConstructor().newInstance();
        Object proxyObject =  Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ModularInvocationHandler(implObject)
        );
        return proxyObject;
    }

    public static class ModularInvocationHandler implements InvocationHandler {
        private final Object target;

        public ModularInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            log.debug("Before method call");
            Object result = method.invoke(target, args);
            log.debug("After method call");
            return result;
        }
    }
}
