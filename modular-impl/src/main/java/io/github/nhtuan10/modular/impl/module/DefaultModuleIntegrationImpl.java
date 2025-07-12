package io.github.nhtuan10.modular.impl.module;

import com.esotericsoftware.kryo.kryo5.objenesis.Objenesis;
import com.esotericsoftware.kryo.kryo5.objenesis.ObjenesisStd;
import io.github.nhtuan10.modular.api.exception.ServiceInvocationRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleIntegration;
import io.github.nhtuan10.modular.impl.serdeserializer.KryoSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DefaultModuleIntegrationImpl implements ModuleIntegration {
    private static Objenesis objenesis = new ObjenesisStd();
    private static final ModuleIntegration INSTANCE = new DefaultModuleIntegrationImpl();
    private final Map<String, QueueHolder> queues = new ConcurrentHashMap<>();

    public static ModuleIntegration getInstance() {
        return INSTANCE;
    }

    @Override
    public <T> BlockingQueue<T> getBlockingQueue(String name, Class<T> type) {
        return (BlockingQueue<T>) getQueue(name, type, LinkedBlockingQueue.class);
    }

    @Override
    public <T> Queue<T> getQueue(String name, Class<T> type) {
        return getQueue(name, type, ConcurrentLinkedQueue.class);
    }

    @Override
    public <T> Queue<T> getQueue(String name, Class<T> type, Class<? extends Queue> queueClass) {
        ClassLoader classLoader = type.getClassLoader();
        QueueHolder queueHolder = queues.computeIfAbsent(name, k -> {
            try {
                final Queue<byte[]> queue = queueClass.getConstructor().newInstance();
                QueueHolder q = new QueueHolder(queue, k);
                q.getQueues().put(classLoader, createProxyQueue(queue, type));
                return q;
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException |
                     InstantiationException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
        return (Queue<T>) queueHolder.getQueues().computeIfAbsent(classLoader, c -> {
            try {
                return createProxyQueue(queueHolder.getQueue(), type);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> Queue<T> createProxyQueue(Queue<byte[]> queue, Class clazz) throws NoSuchFieldException, IllegalAccessException {
        QueueInvocationInterceptor queueInvocationInterceptor = new QueueInvocationInterceptor(queue, new KryoSerDeserializer(clazz.getClassLoader()), clazz);
        Class<? extends Queue> c = new ByteBuddy()
                .subclass(queue.getClass())
                //                .name(apiClass.get() + "$Proxy") // will uncomment it out when does the Graalvm POC
                .method(ElementMatchers.any())
//                .method(ElementMatchers.isDeclaredBy(Queue.class))
                .intercept(MethodDelegation.to(queueInvocationInterceptor))
                .defineField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
                .make()
                .load(clazz.getClassLoader())
                .getLoaded();
        Queue proxy = objenesis.getInstantiatorOf(c).newInstance();
        Field targetField = c.getDeclaredField(DefaultModuleLoader.PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, queue);
        return proxy;
    }

    @RequiredArgsConstructor
    public static class QueueInvocationInterceptor {
        final Queue<byte[]> queue;
        final SerDeserializer serDeserializer;
        final Class<?> clazz;

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method) {
            Object[] convertedArgs = new Object[0];
            if (allArguments.length == 1 && List.of("add", "offer", "put").contains(method.getName())) {
                convertedArgs = Arrays.stream(allArguments).map(serDeserializer::serialization).toArray();
            }
            try {
                Object result = method.invoke(queue, convertedArgs);
                if (result instanceof byte[] bytes && List.of("poll", "remove", "take", "element", "peek").contains(method.getName())) {
                    return serDeserializer.deserialization(bytes, clazz);
                } else {
                    return result;
                }
            } catch (Exception e) {
                throw new ServiceInvocationRuntimeException("Failed to invoke method '%s' in class '%s'".formatted(method, clazz), e);
            }
        }
    }

    @RequiredArgsConstructor
    public static class QueueHolder {
        @Getter
        private final Queue<byte[]> queue;
        private final String name;
        @Getter
        private final Map<ClassLoader, Queue<?>> queues = new ConcurrentHashMap<>();
    }
}
