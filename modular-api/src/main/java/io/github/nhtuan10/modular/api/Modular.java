package io.github.nhtuan10.modular.api;

import io.github.nhtuan10.modular.api.module.ExternalContainer;
import io.github.nhtuan10.modular.api.module.ModuleIntegration;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.api.module.ModuleLoader;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public interface Modular {
    static ModuleLoader.ModuleDetail startModuleSync(String moduleName, List<String> locationUris, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleSync(moduleName, locationUris, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleSyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startModuleAsync(String moduleName, List<String> locationUris, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleAsync(moduleName, locationUris, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleAsyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleSyncWithMainClassLoop(moduleName, locationUris, mainClass, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleSyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleAsyncWithMainClassLoop(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan) {
        return ModuleLoader.getInstance().startSpringModuleAsyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startModuleSync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        return ModuleLoader.getInstance().startModuleSync(moduleName, moduleLoadConfiguration);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startModuleASync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        return ModuleLoader.getInstance().startModuleASync(moduleName, moduleLoadConfiguration);
    }

    static boolean unloadModule(String moduleName) {
        return ModuleLoader.getInstance().unloadModule(moduleName);
    }

    static boolean isManaged(Object object){
        return ModuleLoader.isManaged(object);
    }

    static boolean isManaged(Class<?> clazz){
        return ModuleLoader.isManaged(clazz);
    }

    static <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz) {
        return ModuleLoader.getInstance().getModularServicesFromSpring(name, clazz);
    }

    static <I> List<I> getModularServicesFromSpring(Class<I> clazz) {
        return Modular.getModularServicesFromSpring(null, clazz);
    }

    static <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, String moduleName) {
        return ModuleLoader.getInstance().getModularServicesFromSpring(name, clazz, moduleName);
    }

    static <I> List<I> getModularServices(Class<I> clazz) {
        return ModuleLoader.getInstance().getModularServices(clazz);
    }

    static <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, boolean copyTransClassLoaderObjects) {
        return ModuleLoader.getInstance().getModularServicesFromSpring(name, clazz, copyTransClassLoaderObjects);
    }

    static <I> List<I> getModularServicesFromSpring(Class<I> clazz, boolean copyTransClassLoaderObjects) {
        return Modular.getModularServicesFromSpring(null, clazz, copyTransClassLoaderObjects);
    }

    static <I> List<I> getModularServices(Class<I> clazz, boolean copyTransClassLoaderObjects) {
        return ModuleLoader.getInstance().getModularServices(clazz, copyTransClassLoaderObjects);
    }

    static <I> List<I> getModularServices(Class<I> clazz, String moduleName) {
        return ModuleLoader.getInstance().getModularServices(clazz, moduleName);
    }

    static <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer, boolean copyTransClassLoaderObjects) {
        return ModuleLoader.getInstance().getModularServices(name, clazz, moduleName, externalContainer, copyTransClassLoaderObjects);
    }

    static <I> List<I> getModularServices(String name, Class<I> clazz, ExternalContainer externalContainer) {
        return ModuleLoader.getInstance().getModularServices(name, clazz, externalContainer);
    }

    static <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer) {
        return ModuleLoader.getInstance().getModularServices(name, clazz, moduleName, externalContainer);
    }

    static <T> Queue<T> getQueue(String name, Class<T> clazz) {
        return ModuleIntegration.getInstance().getQueue(name, clazz);
    }

    static <T> Queue<T> getQueue(String name, Class<T> clazz, Class<? extends Queue<T>> queueClass) {
        return ModuleIntegration.getInstance().getQueue(name, clazz, queueClass);
    }

    static <T> BlockingQueue<T> getBlockingQueue(String name, Class<T> clazz) {
        return ModuleIntegration.getInstance().getBlockingQueue(name, clazz);
    }

    static <T> BlockingQueue<T> getBlockingQueue(String name, Class<T> clazz, Class<? extends BlockingQueue> queueClass) {
        return (BlockingQueue<T>) ModuleIntegration.getInstance().getQueue(name, clazz, queueClass);
    }
}
