package com.salesinvoicetools.viewmodels;

import com.google.gson.annotations.Expose;
import com.salesinvoicetools.models.ApiAccess;

import java.util.List;
import java.util.Map;

public class AppDataExport {

    @Expose
    public Map<String,String> businessData;

    @Expose
    public List<ApiAccess> apiAccessData;
}
