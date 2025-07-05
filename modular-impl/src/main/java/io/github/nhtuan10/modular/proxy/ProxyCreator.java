package io.github.nhtuan10.modular.proxy;

import io.github.nhtuan10.modular.module.ModuleLoaderImpl;
import io.github.nhtuan10.modular.serdeserializer.SerDeserializer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ProxyCreator {
    public static <I> I createProxyObject(Class<I> apiClass, Object service, SerDeserializer serDeserializer, boolean copyTransClassLoaderObjects) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        ClassLoader apiClassLoader = apiClass.getClassLoader();
        if (apiClassLoader == null)
            return (I) service;
        Object svcInvocationInterceptor = Class.forName(ServiceInvocationInterceptor.class.getName(), true, apiClassLoader)
                .getConstructor(Object.class, SerDeserializer.class, boolean.class).newInstance(service, serDeserializer, copyTransClassLoaderObjects);
        Object equalsMethodInterceptor = Class.forName(ServiceInvocationInterceptor.EqualsMethodInterceptor.class.getName(), true, apiClassLoader)
                .getConstructor(Object.class).newInstance(service);
        Class<? extends I> c = new ByteBuddy()
                .subclass(apiClass)
                //                .name(apiClass.get() + "$Proxy") // will uncomment it out when does the Graalvm POC
                .method(ElementMatchers.isEquals())
                .intercept(MethodDelegation.to(equalsMethodInterceptor))
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isEquals())))
                .intercept(MethodDelegation.to(svcInvocationInterceptor))
                .defineField(ModuleLoaderImpl.PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
                .make()
                .load(apiClassLoader)
                .getLoaded();
        I proxy = c.getConstructor(new Class[]{}).newInstance();
        Field targetField = c.getDeclaredField(ModuleLoaderImpl.PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, service);
        return proxy;

    }
}
