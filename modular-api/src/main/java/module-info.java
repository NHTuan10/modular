module io.github.nhtuan10.modular {
    exports io.github.nhtuan10.modular.api;
    exports io.github.nhtuan10.modular.context;
    exports io.github.nhtuan10.modular.api.annotation;
    exports io.github.nhtuan10.modular.api.exception;
    exports io.github.nhtuan10.modular.api.module;
    exports io.github.nhtuan10.modular.api.model;
    exports io.github.nhtuan10.modular.api.classloader;
    exports io.github.nhtuan10.modular.serdeserializer;
    exports io.github.nhtuan10.modular.proxy;
    exports io.github.nhtuan10.modular.experimental;
//    requires com.esotericsoftware.kryo.kryo5;
    requires static lombok;
    requires org.slf4j;
    requires net.bytebuddy;
    requires org.apache.commons.lang3;
//    requires org.objenesis;
//    requires com.esotericsoftware.kryo.kryo5;
//    requires com.esotericsoftware.kryo.kryo5;
}