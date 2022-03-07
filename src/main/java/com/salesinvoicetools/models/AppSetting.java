package com.salesinvoicetools.models;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AppSetting {


    @Id
    public String identifier;

    public String val;


    public AppSetting() {}

    public AppSetting(String key, String value) {
        this.identifier = key;
        this.val = value;
    }
}
