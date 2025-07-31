module io.github.nhtuan10.modular.impl {
//    requires com.esotericsoftware.kryo.kryo5;
//    requires com.fasterxml.jackson.dataformat.smile;
    requires io.github.classgraph;
    requires static lombok;
    requires net.bytebuddy;
    requires org.apache.commons.lang3;
//    requires transitive shrinkwrap.resolver.api.maven;
//    requires shrinkwrap.resolver.api;
//    requires shrinkwrap.resolver.api.maven.archive;
//    requires shrinkwrap.resolver.spi;
    requires io.github.nhtuan10.modular;
    requires org.slf4j;
//    requires shrinkwrap.resolver.api.maven;
//    opens io.github.nhtuan10.modular.impl.proxy;
//    opens io.github.nhtuan10.modular.impl.annotation;
    opens io.github.nhtuan10.modular.impl.module to io.github.nhtuan10.modular;
//    opens io.github.nhtuan10.modular.impl.model;
//    opens io.github.nhtuan10.modular.impl.serdeserializer;
//    opens io.github.nhtuan10.modular.impl.classloader;
//    opens io.github.nhtuan10.modular.impl.context to io.github.nhtuan10.modular;
    opens io.github.nhtuan10.modular.impl.context;
    exports io.github.nhtuan10.modular.impl.classloader;
    exports io.github.nhtuan10.modular.impl.proxy;
//    exports io.github.nhtuan10.modular.impl.module;
}