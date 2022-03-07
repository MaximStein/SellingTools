package com.salesinvoicetools.models;

import com.salesinvoicetools.dataaccess.AppSettings;
import com.salesinvoicetools.dataaccess.DataAccessBase;
import com.salesinvoicetools.utils.AppUtils;
import org.apache.commons.io.IOUtils;

import javax.persistence.*;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class StockItem {
    @Id
    @GeneratedValue
    public long id;

    public String title;

    public String subtitle;

    public String description;

    public String htmlDescription;

   // public String images;

    @OneToMany(mappedBy = "owner", cascade = { CascadeType.ALL })
    public List<StockItemVariation> variations = new ArrayList<>();

    @OneToMany(mappedBy = "targetStockItem", cascade = { CascadeType.ALL })
    public List<StockItemAttribute> attributes = new ArrayList<>();

    public String customData;

    public Collection<StockItemAttribute> getPrices() {
        return attributes.stream().filter(a -> a.attributeKey == "price").collect(Collectors.toList());
    }

    public long[] getPriceRange() {

        var prices =  getPrices().stream().map(p -> p.getLong());
        var max = prices.max(Long::compareTo);
        var min = prices.min(Long::compareTo);

        return new long[] {min.get(), max.get()};
    }

    public File[] getLocalImages() {
        return new File(getLocalImagesPath()).listFiles();
    }


    public String getLocalImagesPath() {
        var appDir = AppSettings.getString(AppSettings.APP_DATA_DIRECTORY);
        return (appDir == null ? "" : appDir+"/")+"product-images/"+id;
    }

    public Long getPrice(Map<String,String> variation){
        var attr = getAttribute(variation,"price");
        return attr == null ? null : attr.getLong();
    }

    public void setPrice(Map<String,String> variation, long price) {
        setAttribute(variation, "price", ""+price);
    }

    public StockItemAttribute getAttribute(Map<String,String> variation, String key) {
        var attr = attributes.stream().filter(
                a -> a.attributeKey.equals(key)
                        && (variation == null && a.targetVariation == null
                            || AppUtils.jsonToMap(a.targetVariation).equals(variation))
        ).findFirst();

        return attr.isEmpty() ? null : attr.get();
    }

    public void setAttribute(Map<String,String> targetVariation, String key, String val) {
        var existing =  getAttribute(targetVariation, key);

        if(existing == null) {
            existing = new StockItemAttribute();
            attributes.add(existing);
            existing.targetStockItem = this;
            existing.attributeKey = key;

        }
        else {
            if(val == null || val.equals("null")) {
                DataAccessBase.delete(existing);
                return;
            }
        }

        existing.attributeVal = val;
        existing.targetVariation = AppUtils.mapToJson(targetVariation);

        DataAccessBase.insertOrUpdate(existing);


    }
}
