package com.salesinvoicetools.shopapis.etsy;

import com.salesinvoicetools.utils.AppUtils;

import java.util.List;

public  final class Pojos {


    public class OAuth2TokenResponse {
        public String access_token;
        public String token_type;
        public long expires_in;
        public String refresh_token;
    }

    public  class ReceiptResponse {
        public int count;
        public List<Receipt> results;
    }

    public class ShopsResponse {
        public int count;
        public List<Shop> results;
    }

    public class MoneyAmount{
        public long amount;
        public long divisor;
        public String currency_code;

        @Override
        public String toString() {
            return AppUtils.formatCurrencyAmount(amount);
        }
    }

    public class Variation{
        public long property_id;
        public long value_id;
        public String formatted_name;
        public String formatted_value;
    }

    public class Transaction{

        @Override
        public String toString() {
            return "* "+quantity+" x "+price+" "+" | "+title+" *";
        }

        public long transaction_id;
        public String title;
        public String description;
        public long seller_user_id;
        public long buyer_user_id;
        public long create_timestamp;
        public long paid_timestamp;
        public long shipped_timestamp;
        public long quantity;
        public long listing_image_id;
        public long receipt_id;
        public boolean is_digital;
        public String file_data;
        public long listing_id;
        public String transaction_type;
        public long product_id;
        public String sku;
        public MoneyAmount price;
        public MoneyAmount shipping_cost;
        public List<Variation> variations;
        public long shipping_profile_id;
        public long min_processing_days;
        public long max_processing_days;
        public String shipping_method;
        public String shipping_upgrade;
        public long expected_ship_date;
    }

    public class Receipt{

        @Override
        public String toString() {
            return "*"+buyer_email+" : "+grandtotal+"*";
        }

        public long receipt_id;
        public long receipt_type;
        public long seller_user_id;
        public String seller_email;
        public long buyer_user_id;
        public String buyer_email;
        public String name;
        public String first_line;
        public String second_line;
        public String city;
        public String state;
        public String zip;
        public String status;
        public String formatted_address;
        public String country_iso;
        public String payment_method;
        public String payment_email;
        public String message_from_seller;
        public String message_from_buyer;
        public String message_from_payment;
        public boolean is_paid;
        public boolean is_shipped;
        public long create_timestamp;
        public long update_timestamp;
        public String gift_message;
        public MoneyAmount grandtotal;
        public MoneyAmount subtotal;
        public MoneyAmount total_price;
        public MoneyAmount total_shipping_cost;
        public MoneyAmount total_tax_cost;
        public MoneyAmount total_vat_cost;
        public MoneyAmount discount_amt;
        public MoneyAmount gift_wrap_price;
        public List<Object> shipments;
        public List<Transaction> transactions;
    }

    public class Shop{
        public long shop_id;
        public long user_id;
        public String shop_name;
        public long create_date;
        public String title;
        public String announcement;
        public String currency_code;
        public boolean is_vacation;
        public String vacation_message;
        public String sale_message;
        public String digital_sale_message;
        public long update_date;
        public long listing_active_count;
        public long digital_listing_count;
        public String login_name;
        public boolean accepts_custom_requests;
        public String policy_welcome;
        public String policy_payment;
        public String policy_shipping;
        public String policy_refunds;
        public String policy_additional;
        public String policy_seller_info;
        public long policy_update_date;
        public boolean policy_has_private_receipt_info;
        public boolean has_unstructured_policies;
        public String policy_privacy;
        public String vacation_autoreply;
        public String url;
        public String image_url_760x100;
        public long num_favorers;
        public List<String> languages;
        public String icon_url_fullxfull;
        public boolean is_using_structured_policies;
        public boolean has_onboarded_structured_policies;
        public boolean include_dispute_form_link;
        public boolean is_direct_checkout_onboarded;
        public boolean is_etsy_payments_onboarded;
        public boolean is_calculated_eligible;
        public boolean is_opted_in_to_buyer_promise;
        public boolean is_shop_us_based;
        public long transaction_sold_count;
        public String shipping_from_country_iso;
        public String shop_location_country_iso;
        public long review_count;
        public double review_average;
    }
}
