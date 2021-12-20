package com.salesinvoicetools.models;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
public class UserMessage {

    @Id
    @GeneratedValue
    public long id;

    public String imageAttachments;

    public String text;

    public String subject;

    public Timestamp timeSent;

    public String otherUserName;

    public boolean isToOtherUser;

    @ManyToOne
    @JoinColumn(name = "token_id")
    public OAuth2Token token;

    @ManyToOne
    @JoinColumn(name = "product_reference_id")
    public Product productReference;

    @ManyToOne
    @JoinColumn(name = "belongs_to_order_id")
    public ShopOrder belongsToOrder;



}
