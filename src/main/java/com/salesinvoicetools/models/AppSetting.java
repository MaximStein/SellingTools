package com.salesinvoicetools.models;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AppSetting {

    @Id
    public String key;

    public String value;

    public AppSetting() {}

    public AppSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
