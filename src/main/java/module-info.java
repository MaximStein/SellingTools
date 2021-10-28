module com.salesinvoicetools.salesinvoicetools {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires java.sql;
    requires com.github.librepdf.openpdf;
    requires google.http.client;

    requires com.fasterxml.jackson.databind;
    requires scribejava.core;
    requires javafx.web;
    requires de.jensd.fx.glyphs.fontawesome;
    requires com.google.gson;
    requires org.apache.commons.io;
    requires scribejava.apis;


    requires com.h2database;
    requires java.persistence;

    opens com.salesinvoicetools to javafx.fxml, com.h2database, java.persistence;
    opens com.salesinvoicetools.models to javafx.fxml, com.h2database, java.persistence;
    opens com.salesinvoicetools.controllers to javafx.fxml;
    exports com.salesinvoicetools;
    exports com.salesinvoicetools.models;
    exports com.salesinvoicetools.controllers;
}