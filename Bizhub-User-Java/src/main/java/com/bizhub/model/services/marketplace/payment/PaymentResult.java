package com.bizhub.model.services.marketplace.payment;

public class PaymentResult {

    private final boolean success;
    private final String paymentUrl;
    private final String ref;
    private final String errorMessage;

    private PaymentResult(boolean success, String ref, String paymentUrl, String errorMessage) {
        this.success = success;
        this.ref = ref;
        this.paymentUrl = paymentUrl;
        this.errorMessage = errorMessage;
    }

    public static PaymentResult ok(String ref, String url) {
        return new PaymentResult(true, ref, url, null);
    }

    public static PaymentResult fail(String errorMessage) {
        return new PaymentResult(false, null, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public String getPaymentUrl() { return paymentUrl; }
    public String getRef() { return ref; }
    public String getErrorMessage() { return errorMessage; }

    // ✅ compat si tu appelles getUrl() dans le controller
    public String getUrl() { return paymentUrl; }

    // ✅ compat si ton TestStripe appelle getSessionId()
    public String getSessionId() { return ref; }
}