module io.github.nhtuan10.sample.plugin1 {
    requires com.fasterxml.jackson.databind;
    requires io.github.classgraph;
    requires static lombok;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires io.github.nhtuan10.modular;
    requires io.github.nhtuan10.sample.api.service;
//    requires kafka.clients;
//    requires kafka.clients;
//    requires static kafka.schema.serializer;
//    requires static kafka.schema.registry.client;

    opens io.github.nhtuan10.sample.plugin1 to io.github.nhtuan10.modular, io.github.nhtuan10.modular.impl;
    exports io.github.nhtuan10.sample.plugin1 to io.github.nhtuan10.modular, io.github.nhtuan10.modular.impl;
}