package com.keepSafe911.listner

interface PaymentOptionListener {
    fun onCreditCardOption()
    fun onPayPalOption(subscriptionId: String, firstName: String = "", lastName: String = "", email: String = "")
    fun onPaymentExpired() {}
}