package io.github.nhtuan10.modular.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac)
            throws BeansException {
        context = ac;
    }

    public static Object getBean(Class requiredType) {
        return context.getBean(requiredType);
    }


    public static void registerBean(String beanName, Object bean) {
        ((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton(beanName, bean);
    }

//    @EventListener
//    public void handleContextRefreshEvent(ContextRefreshedEvent ctxStartEvt) {
//        System.out.println("Context Start Event received.");
//    }
//
//    @Override
//    public void onApplicationEvent(ContextRefreshedEvent event) {
//        System.out.println("Context Start Event received onApplicationEvent.");
//
//    }
}