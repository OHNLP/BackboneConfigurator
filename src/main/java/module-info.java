open module org.ohnlp.backbone.configurator {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.fasterxml.jackson.databind;
    requires org.apache.beam.sdk;
    requires org.ohnlp.backbone.api;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires java.logging;
    requires java.sql;
    requires fxgraph;
    requires org.abego.treelayout.core;
    requires java.desktop;
    requires org.kordamp.ikonli.javafx;
    requires spring.web;


    exports org.ohnlp.backbone.configurator;
    exports org.ohnlp.backbone.configurator.structs.modules;
    exports org.ohnlp.backbone.configurator.structs.modules.types;
    exports org.ohnlp.backbone.configurator.structs.pipeline;
    exports org.ohnlp.backbone.configurator.gui.controller;
    exports org.ohnlp.backbone.configurator.gui;
    exports org.ohnlp.backbone.configurator.structs.modules.serde;
    exports org.ohnlp.backbone.configurator.util;
}