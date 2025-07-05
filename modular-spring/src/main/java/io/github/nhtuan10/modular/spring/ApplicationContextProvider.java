package io.github.nhtuan10.modular.spring;

import io.github.nhtuan10.modular.ModularContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @EventListener
    public void handleContextRefreshEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("ContextRefreshedEvent received.");
        ModularContext.notifyModuleReady();
    }

    @Override
    public void setApplicationContext(ApplicationContext ac)
            throws BeansException {
        context = ac;
    }

    public static Object getBean(Class<?> requiredType) {
        return context.getBean(requiredType);
    }

    public static Object getBean(String beanName, Class<?> requiredType) {
        return context.getBean(beanName, requiredType);
    }


    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }

    public static void registerBean(String beanName, Object bean) {
        ((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton(beanName, bean);
    }
}