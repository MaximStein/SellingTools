module com.salesinvoicetools.salesinvoicetools {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.base;
    requires javafx.media;


    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires java.sql;
    requires com.github.librepdf.openpdf;
    requires google.http.client;

    requires com.fasterxml.jackson.databind;
    requires de.jensd.fx.glyphs.fontawesome;
    requires scribejava.core;
    requires com.google.gson;
    requires scribejava.apis;
    requires org.apache.commons.io;


    requires com.h2database;
    requires java.persistence;
    requires com.google.common;


    opens com.salesinvoicetools;
    opens com.salesinvoicetools.models;
    opens com.salesinvoicetools.controllers;
    opens com.salesinvoicetools.dataaccess.models;
    opens com.salesinvoicetools.utils;
    opens com.salesinvoicetools.shopapis;

    exports com.salesinvoicetools;
    exports com.salesinvoicetools.models;
    exports com.salesinvoicetools.controllers;
    exports  com.salesinvoicetools.dataaccess.models;
    exports  com.salesinvoicetools.utils;

    }