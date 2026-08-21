package io.github.nhtuan10.modular.impl.proxy;

import com.esotericsoftware.kryo.kryo5.objenesis.Objenesis;
import com.esotericsoftware.kryo.kryo5.objenesis.ObjenesisStd;
import io.github.nhtuan10.modular.impl.module.DefaultModuleLoader;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ServiceProxyCreator {
    private static final Objenesis objenesis = new ObjenesisStd();

    public static <I> I createProxyObject(Class<I> apiClass, Object service, SerDeserializer serDeserializer, boolean copyTransClassLoaderObjects,
                                          ClassLoader sourceClassLoader, ClassLoader targetClassLoader) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
//        ClassLoader sourceClassLoader = apiClass.getClassLoader();
        if (sourceClassLoader == null)
            sourceClassLoader = ClassLoader.getPlatformClassLoader();

        Object svcInvocationInterceptor = Class.forName(DefaultServiceInvocationInterceptor.class.getName(), true, sourceClassLoader)
                .getConstructor(Object.class, SerDeserializer.class, boolean.class, ClassLoader.class, ClassLoader.class).newInstance(service, serDeserializer, copyTransClassLoaderObjects, sourceClassLoader, targetClassLoader);
        Object equalsMethodInterceptor = Class.forName(DefaultServiceInvocationInterceptor.EqualsMethodInterceptor.class.getName(), true, sourceClassLoader)
                .getConstructor(Object.class).newInstance(service);

//        String sha256Hex = DigestUtils.sha256Hex(apiClass.getName() + "$" + service.getClass().getName() + "$Proxy");
//        String className = "modular." + apiClass.getName() + "$Proxy$" + sha256Hex.substring(0, 8);
//        Class<? extends I> c;
//
//        try {
//            Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
//            method.setAccessible(true); // Bypass protected access modifier
//            c = (Class<? extends I>) method.invoke(sourceClassLoader, className);
//        } catch (Exception e) {
//            c = null;
//        }
//
//        if (c == null) {
        Class<? extends I> c = new ByteBuddy()
                .subclass(apiClass)
//                    .name(className)
                .method(ElementMatchers.isEquals())
                .intercept(MethodDelegation.to(equalsMethodInterceptor))
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isEquals())))
                .intercept(MethodDelegation.to(svcInvocationInterceptor))
                .defineField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
//                .defineConstructor(Visibility.PUBLIC)
//                .constructor(ElementMatchers.any())
//                .intercept(MethodCall.invoke(Object.class.getConstructor()) // Call super()
//                        .andThen(FieldAccessor.ofField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME).setsValue(service)))
                .make()
                .load(sourceClassLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();

        I proxy = objenesis.getInstantiatorOf(c).newInstance();
        Field targetField = c.getDeclaredField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, service);
        return proxy;

    }

    static boolean isBoxedPrimitive(Class<?> type) {
        return type == Integer.class ||
                type == Long.class ||
                type == Short.class ||
                type == Byte.class ||
                type == Float.class ||
                type == Double.class ||
                type == Boolean.class ||
                type == Character.class;
    }

    public static boolean isConversionNotNeeded(Object obj, Class<?> type, ClassLoader sourceClassLoader, ClassLoader targetClassLoader) {
        //        if (!noCast) {
//            ClassLoader classLoader = targetClassLoader;
//            while (classLoader != null) {
//                classLoader = classLoader.getParent();
//                if (sourceClassLoader == classLoader) {
//                    noCast = true;
//                }
//            }
//        }
        return ((sourceClassLoader == targetClassLoader) || (obj == null) || type.isPrimitive() || isBoxedPrimitive(type) || type.equals(String.class));
    }
}
