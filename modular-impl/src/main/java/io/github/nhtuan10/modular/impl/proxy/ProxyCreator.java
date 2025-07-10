package io.github.nhtuan10.modular.impl.proxy;

import com.esotericsoftware.kryo.kryo5.objenesis.Objenesis;
import com.esotericsoftware.kryo.kryo5.objenesis.ObjenesisStd;
import io.github.nhtuan10.modular.impl.module.DefaultModuleLoader;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ProxyCreator {
    private static Objenesis objenesis = new ObjenesisStd();
    public static <I> I createProxyObject(Class<I> apiClass, Object service, SerDeserializer serDeserializer, boolean copyTransClassLoaderObjects,
                                          ClassLoader sourceClassLoader, ClassLoader targetClassLoader) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
//        ClassLoader sourceClassLoader = apiClass.getClassLoader();
        if (sourceClassLoader == null)
            sourceClassLoader = ClassLoader.getPlatformClassLoader();

        Object svcInvocationInterceptor = Class.forName(ServiceInvocationInterceptor.class.getName(), true, sourceClassLoader)
                .getConstructor(Object.class, SerDeserializer.class, boolean.class, ClassLoader.class, ClassLoader.class).newInstance(service, serDeserializer, copyTransClassLoaderObjects, sourceClassLoader, targetClassLoader);
        Object equalsMethodInterceptor = Class.forName(ServiceInvocationInterceptor.EqualsMethodInterceptor.class.getName(), true, sourceClassLoader)
                .getConstructor(Object.class).newInstance(service);
        Class<? extends I> c = new ByteBuddy()
                .subclass(apiClass)
                //                .name(apiClass.get() + "$Proxy") // will uncomment it out when does the Graalvm POC
                .method(ElementMatchers.isEquals())
                .intercept(MethodDelegation.to(equalsMethodInterceptor))
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isEquals())))
                .intercept(MethodDelegation.to(svcInvocationInterceptor))
                .defineField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
                .make()
                .load(sourceClassLoader)
                .getLoaded();
        I proxy = objenesis.getInstantiatorOf(c).newInstance();
        Field targetField = c.getDeclaredField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, service);
        return proxy;

    }
}
