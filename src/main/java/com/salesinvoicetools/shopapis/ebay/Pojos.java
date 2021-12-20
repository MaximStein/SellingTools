package com.salesinvoicetools.shopapis.ebay;

import com.salesinvoicetools.models.LineItem;
import com.salesinvoicetools.models.Product;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class Pojos {

    public class OrderSearchPagedCollection {
        int limit;
        int total;
        String next;
        int offset;
        EbayOrder[] orders;
        ErrorDetailV3[] warnings;
    }

    public class ShippingFulfillmentPagedCollection {
        ShippingFulfillment[] fulfillments;
        int total;
        ErrorDetailV3[] warnings;
    }

    public class ErrorDetailV3 {
        String category;
        String domain;
        int errorId;
        String[] inputRefIds;
        String longMessage;
        String message;
        String[] outputRefIds;
        String subDomain;
    }

    public class ShippingFulfillment {
        String fulfillmentId;
        LineItem[] lineItems;
        String shipmentTrackingNumber;
        Date shippedDate;
        String shippingCarrierCode;
    }


    public static class ShipToLocationAvailability {
        int quantity;

    }

    public static class Availability {
        ShipToLocationAvailability shipToLocationAvailability;
    }

    static class EbayProduct {
        String brand;
        String title;
        String description;
        HashMap<String, String[]> aspects;
        String product;
        String mpn;
        String[] imageUrls;
    }

    public static class IntenvoryItem {
        Availability availability;
        String condition;
        EbayProduct product;
    }

    public class EbayOrder {

        String orderId;
        Date creationDate;
        Date lastModifiedDate;
        String orderFulfillmentStatus;
        String orderPaymentStatus;
        String sellerId;
        String buyerCheckoutNotes;
        PricingSummary pricingSummary;
        CancelStatus cancelStatus;
        PaymentSummary paymentSummary;
        LineItem[] lineItems;
        FulfillmentStartInstruction[] fulfillmentStartInstructions;
        String[] fulfillmentHrefs;
        Buyer buyer;

        public class Buyer {
            String username;
            Addr taxAddress;
        }

        public class CancelStatus {
            public String cancelState;

        }

        public class LineItem {
            String lineItemId;
            String legacyItemId;
            String legacyVariationId;
            String title;
            int quantity;
            Amount total;
            DeliveryCost deliveryCost;
            Amount lineItemCost;
            Amount discountedLineItemCost;
        }

        public class Amount {
            double convertedFromValue;
            String convertedFromCurrency;
            double value;
            String currency;
        }

        public class DeliveryCost {
            Amount shippingCost;
        }

        public class PricingSummary {

            Amount fee;
            Amount priceSubtotal;
            Amount tax;
            Amount pricingSummary;
            Amount deliveryCost;
            Amount total;
        }

        public class PaymentSummary {
            Amount totalDueSeller;
            Payment[] payments;
            Amount[] refunds;

        }

        public class Payment {
            String paymentMethod;
            String paymentReferenceId;
            Amount amount;
            String paymentStatus;
        }

        public class FulfillmentStartInstruction {
            String fulfillmentInstructionsType;
            Date minEstimatedDeliveryDate;
            Date maxEstimatedDeliveryDate;
            boolean ebaySupportedFulfillment;
            ShippingStep shippingStep;
        }

        public class ShippingStep {
            String shippingCarrierCode;
            String shippingServiceCode;
            Person shipTo;
        }

        public class Person {
            String fullName;
            Addr contactAddress;
            String email;
            Phone primaryPhone;
        }

        public  class Phone {
            String phoneNumber;
        }

        public  class Addr {
            String addressLine1;
            String addressLine2;
            String city;
            String postalCode;
            String countryCode;
        }
        public class Tax{
            public String ebayCollectAndRemitTax;
            public String includedInPrice;
            public String shippingAndHandlingTaxed;
            public TaxJurisdiction taxJurisdiction;
            public String taxPercentage;
            public String taxType;
        }

        public class ShippingOption{
            public Amount additionalShippingCostPerUnit;
            public String cutOffDateUsedForEstimate;
            public String fulfilledThrough;
            public String guaranteedDelivery;
            public Amount importCharges;
            public String maxEstimatedDeliveryDate;
            public String minEstimatedDeliveryDate;
            public String quantityUsedForEstimate;
            public String shippingCarrierCode;
            public Amount shippingCost;
            public String shippingCostType;
            public String shippingServiceCode;
            public ShipToLocationUsedForEstimate shipToLocationUsedForEstimate;
            public String trademarkSymbol;
            public String type;
        }

        public class ShipToLocationUsedForEstimate{
            public String country;
            public String postalCode;
        }
        public class TaxJurisdiction{
            public Region region;
            public String taxJurisdictionId;
        }
        public class ShipToLocations{
            public List<Region> regionExcluded;
            public List<Region> regionIncluded;
        }
        public class Region{
            public String regionId;
            public String regionName;
            public String regionType;
        }

        public class Parameter{
            public String name;
            public String value;
        }

        public class Image {
            public String height;
            public String imageUrl;
            public String width;
        }

        public  class PaymentMethod{
            public String paymentMethodType;
            public List<PaymentMethodBrand> paymentMethodBrands;
            public List<String> paymentInstructions;
            public List<String> sellerInstructions;
        }

        public class PaymentMethodBrand{
            public String paymentMethodBrandType;
            public Image logoImage;
        }

        public class Item {
            public List<Image> additionalImages;
            public String adultOnly;
            public String ageGroup;
            public List<String> availableCoupons;
            public String bidCount;
            public String brand;
            public List<String> buyingOptions;
            public String categoryId;
            public String categoryPath;
            public String color;
            public String condition;
            public String conditionDescription;
            public String conditionId;
            public Amount currentBidPrice;
            public String description;
            public String eligibleForInlineCheckout;
            public String enabledForGuestCheckout;
            public String energyEfficiencyClass;
            public String epid;
            public String gender;
            public String gtin;
            public Image image;
            public String inferredEpid;
            public String itemAffiliateWebUrl;
            public String itemEndDate;
            public String itemId;
            public Addr itemLocation;
            public String itemWebUrl;
            public String legacyItemId;

            public String lotSize;
            public Amount marketingPrice;
            public String material;
            public Amount minimumPriceToBid;
            public String mpn;
            public String pattern;
            public List<PaymentMethod> paymentMethods;
            public Amount price;
            public String priceDisplayCondition;
            //	public PrimaryItemGroup primaryItemGroup;
            public ProductReviewRating primaryProductReviewRating;
            public String priorityListing;
            public Product product;
            public String productFicheWebUrl;
            public List<String> qualifiedPrograms;
            public String quantityLimitPerBuyer;
            public String reservePriceMet;
            public ReturnTerms returnTerms;
            public Seller seller;
            public String sellerItemRevision;
            public List<ShippingOption> shippingOptions;
            public ShipToLocations shipToLocations;
            public String shortDescription;
            public String size;
            public String sizeSystem;
            public String sizeType;
            public String subtitle;
            public List<Tax> taxes;
            public String title;
            public String topRatedBuyingExperience;
            public String tyreLabelImageUrl;
            public String uniqueBidderCount;
            public Amount unitPrice;
            public String unitPricingMeasure;
            public List<ErrorDetailV3> warnings;
            public String watchCount;
        }

        public class ErrorResponse {
            List<ErrorDetailV3> errors;
        }

        public  class TimeDuration{
            public String unit;
            public String value;
        }

        public  class ReturnTerms{
            public String extendedHolidayReturnsOffered;
            public String refundMethod;
            public String restockingFeePercentage;
            public String returnInstructions;
            public String returnMethod;
            public TimeDuration returnPeriod;
            public String returnsAccepted;
            public String returnShippingCostPayer;
        }

        public class ProductReviewRating{
            public String averageRating;
            public List<RatingHistogram> ratingHistograms;
            public String reviewCount;
        }
        public class SellerLegalInfo{
            public String email;
            public String fax;
            public String imprint;
            public String legalContactFirstName;
            public String legalContactLastName;
            public String name;
            public String phone;
            public String registrationNumber;
            public Addr sellerProvidedLegalAddress;
            public String termsOfService;
            public List<VatDetail> vatDetails;
        }

        public class VatDetail {
            public String issuingCountry;
            public String vatId;
        }
        public class Seller{
            public String feedbackPercentage;
            public String feedbackScore;
            public String sellerAccountType;
            public SellerLegalInfo sellerLegalInfo;
            public String username;
        }


        class RatingHistogram{
            public String count;
            public String rating;
        }

        public class ItemGroup{
            public List<CommonDescription> commonDescriptions;
            public List<Item> items;
            public List<ErrorDetailV3> warnings;
        }

        public class CommonDescription{
            public String description;
            public List<String> itemIds;
        }

    }

}
