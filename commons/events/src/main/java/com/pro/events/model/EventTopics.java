package com.pro.events.model;

public class EventTopics {

    private EventTopics() {}

    /**
     * Order topics
     */
    public static final String ORDER_CREATED   = "order.created";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_SHIPPED   = "order.shipped";
    public static final String ORDER_DELIVERED = "order.delivered";

    /**
     * Message / Notification topics
     */
    public static final String NOTIFICATION_EMAIL = "notification.email";
    public static final String NOTIFICATION_SMS   = "notification.sms";
    public static final String NOTIFICATION_PUSH  = "notification.push";

}
