module io.github.nhtuan10.modular {
    exports io.github.nhtuan10.modular.api;
    exports io.github.nhtuan10.modular.context;
    exports io.github.nhtuan10.modular.api.annotation;
    exports io.github.nhtuan10.modular.api.exception;
    exports io.github.nhtuan10.modular.api.module;
    exports io.github.nhtuan10.modular.api.model;
    exports io.github.nhtuan10.modular.api.classloader;
    exports io.github.nhtuan10.modular.api.serdeserializer;
    requires static lombok;
    requires net.bytebuddy;
    requires org.apache.commons.lang3;
}