package com.expensevault.platform.notification

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TransactionNotificationParserTest {

    @Test
    fun parseGooglePayNotification() {
        val title = "Google Pay"
        val text = "Paid ₹150.00 to Chai Point using HDFC Bank account"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("150.00"), result.amount)
        assertEquals("Chai Point", result.merchant)
        assertEquals("Food", result.suggestedCategoryName)
    }

    @Test
    fun parsePhonePeNotification() {
        val title = "Payment Successful"
        val text = "Paid ₹429 to Swiggy. Debited from State Bank of India A/c ending in 4321."
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("429.00"), result.amount)
        assertEquals("Swiggy", result.merchant)
        assertEquals("4321", result.accountSuffix)
        assertEquals("Food", result.suggestedCategoryName)
    }

    @Test
    fun parsePaytmNotification() {
        val title = "Paid ₹250 to Uber"
        val text = "Money sent! ₹250.00 paid to Uber India from Paytm Payments Bank A/c ...1234"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("250.00"), result.amount)
        assertEquals("Uber", result.merchant)
        assertEquals("1234", result.accountSuffix)
        assertEquals("Transport", result.suggestedCategoryName)
    }

    @Test
    fun parseHdfcBankDebitSms() {
        val title = "HDFC Bank Alert"
        val text = "Alert: You have spent INR 1,450.00 on your HDFC Bank Debit Card ending 4567 at AMAZON INDIA on 20-09-2026. Avail bal: INR 25,000"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("1450.00"), result.amount)
        assertEquals("AMAZON", result.merchant)
        assertEquals("4567", result.accountSuffix)
        assertEquals("Shopping", result.suggestedCategoryName)
    }

    @Test
    fun parseSbiDebitSms() {
        val title = "SBI"
        val text = "Dear SBI User, your A/C ending with 9876 debited by Rs 2,500.00 on 20Sep26 by transfer to VPA merchant@sbi (Ref No 123456)."
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("2500.00"), result.amount)
        assertEquals("9876", result.accountSuffix)
    }

    @Test
    fun parseCredNotification() {
        val title = "CRED Payment Alert"
        val text = "Paid Rs. 350 at Starbucks Coffee using ICICI Credit Card xx8001"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("350.00"), result.amount)
        assertEquals("Starbucks Coffee", result.merchant)
        assertEquals("8001", result.accountSuffix)
        assertEquals("Food", result.suggestedCategoryName)
    }

    @Test
    fun parseUsdTransactionNotification() {
        val title = "Chase Bank"
        val text = "You spent $45.50 at Trader Joe's on your Card ending 1234."
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("45.50"), result.amount)
        assertEquals("Trader Joe's", result.merchant)
        assertEquals("1234", result.accountSuffix)
        assertEquals("Groceries", result.suggestedCategoryName)
    }

    @Test
    fun parseEurTransactionNotification() {
        val title = "Revolut"
        val text = "Paid €12.99 to Netflix for monthly subscription"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("12.99"), result.amount)
        assertEquals("Netflix", result.merchant)
        assertEquals("Entertainment", result.suggestedCategoryName)
    }

    @Test
    fun parseGbpTransactionNotification() {
        val title = "Barclays"
        val text = "Alert: £24.00 spent at Tesco on Card ending with 9988"
        val result = TransactionNotificationParser.parse(title, text)

        assertNotNull(result)
        assertEquals(BigDecimal("24.00"), result.amount)
        assertEquals("Tesco", result.merchant)
        assertEquals("9988", result.accountSuffix)
        assertEquals("Groceries", result.suggestedCategoryName)
    }

    @Test
    fun ignoreCreditNotifications() {
        val title = "Bank Alert"
        val text = "INR 10,000.00 credited to your A/C ending 1234 from Salary Deposit"
        val result = TransactionNotificationParser.parse(title, text)

        assertNull(result)
    }

    @Test
    fun ignoreOtpNotifications() {
        val title = "OTP Alert"
        val text = "Your OTP is 894312 for purchase of Rs 500 at Merchant. Do not share."
        val result = TransactionNotificationParser.parse(title, text)

        assertNull(result)
    }
}
