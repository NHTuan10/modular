package io.github.nhtuan10.modular.proxy;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;

@AllArgsConstructor
public class ServiceInvocationInterceptor {
    //  private ModularServiceHolder modularServiceHolder;
    private Object service;
//  public ServiceInvocationInterceptor(ModularServiceHolder modularServiceHolder) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//    this.modularServiceHolder = modularServiceHolder;
//    service = modularServiceHolder.getServiceClass().getConstructor().newInstance();
//
//  }

    @SneakyThrows
    @RuntimeType
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method, @This Object object) {
        // intercept any method of any signature
        Class<?>[] parameterTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .map(s -> {
                    try {
                        return service.getClass().getClassLoader().loadClass(s);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(Class[]::new);
        Method m = service.getClass().getDeclaredMethod(method.getName(), parameterTypes);
        Object[] convertedArgs = IntStream.range(0, parameterTypes.length).mapToObj(i -> {
            return castWithSerialization((Serializable) allArguments[i], parameterTypes[i].getClassLoader());

        }).toArray();

        Serializable result = (Serializable) m.invoke(service, convertedArgs);
        return castWithSerialization(result, this.getClass().getClassLoader());

    }

    public static Object castWithSerialization(Serializable obj, ClassLoader classLoader) {
        byte[] b = SerializationUtils.serialize(obj);
        InputStream is = new ByteArrayInputStream(b);
        try (ObjectInputStream objectInputStream = new ObjectInputStreamWithClassLoader(is, classLoader)) {
            return objectInputStream.readObject();
        } catch (final ClassNotFoundException | IOException | NegativeArraySizeException ex) {
            throw new SerializationException(ex);
        }
    }

    public static class ObjectInputStreamWithClassLoader extends ObjectInputStream {

        ClassLoader classLoader;

        public ObjectInputStreamWithClassLoader(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), true, classLoader);
            } catch (Exception e) {
                return super.resolveClass(desc);
            }
        }
    }

    public static class SerializationException extends RuntimeException {
        public SerializationException(Exception e) {
            super(e);
        }
    }
}