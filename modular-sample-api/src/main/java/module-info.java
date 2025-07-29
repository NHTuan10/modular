module io.github.nhtuan10.sample.api.service {
    exports io.github.nhtuan10.sample.api.service;
    requires io.github.nhtuan10.modular;
    requires static lombok;
//    opens io.github.nhtuan10.sample.api.service to io.github.nhtuan10.modular, io.github.nhtuan10.modular.impl, io.github.nhtuan10.sample.launcher;
    opens io.github.nhtuan10.sample.api.service to io.github.nhtuan10.modular.impl, io.github.nhtuan10.modular;
//    opens io.github.nhtuan10.sample.api.service;
}