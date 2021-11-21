package com.salesinvoicetools.dataaccess;

import com.salesinvoicetools.models.Product;
import com.salesinvoicetools.shopapis.ShopApiBase.*;

import java.util.Map;

public class ProductsDataAccess extends DataAccessBase{

    public static Product getNewOrExisting(Marketplace m, String productNumber) {
        var products = DataAccessBase.<Product>getWhereAnd(Product.class,
                Map.of("productNumber", productNumber, "marketplace", m));

        return products.size() == 0 ? new Product(m,productNumber) : products.get(0);
    }
}
