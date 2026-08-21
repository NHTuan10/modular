package io.github.nhtuan10.modular.impl.proxy;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;

public interface ServiceInvocationInterceptor {
    @RuntimeType
    Object intercept(@AllArguments Object[] allArguments,
                     @Origin Method method) throws Exception;
}
