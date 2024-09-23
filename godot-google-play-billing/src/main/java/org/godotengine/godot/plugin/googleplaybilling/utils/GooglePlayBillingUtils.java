/*************************************************************************/
/*  GooglePlayBillingUtils.java                                                    */
/*************************************************************************/
/*                       This file is part of:                           */
/*                           GODOT ENGINE                                */
/*                      https://godotengine.org                          */
/*************************************************************************/
/* Copyright (c) 2007-2020 Juan Linietsky, Ariel Manzur.                 */
/* Copyright (c) 2014-2020 Godot Engine contributors (cf. AUTHORS.md).   */
/*                                                                       */
/* Permission is hereby granted, free of charge, to any person obtaining */
/* a copy of this software and associated documentation files (the       */
/* "Software"), to deal in the Software without restriction, including   */
/* without limitation the rights to use, copy, modify, merge, publish,   */
/* distribute, sublicense, and/or sell copies of the Software, and to    */
/* permit persons to whom the Software is furnished to do so, subject to */
/* the following conditions:                                             */
/*                                                                       */
/* The above copyright notice and this permission notice shall be        */
/* included in all copies or substantial portions of the Software.       */
/*                                                                       */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*/
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY  */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,  */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE     */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                */
/*************************************************************************/

package org.godotengine.godot.plugin.googleplaybilling.utils;

import org.godotengine.godot.Dictionary;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GooglePlayBillingUtils {
	public static Dictionary convertPurchaseToDictionary(Purchase purchase) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("original_json", purchase.getOriginalJson());
		dictionary.put("order_id", purchase.getOrderId());
		dictionary.put("package_name", purchase.getPackageName());
		dictionary.put("purchase_state", purchase.getPurchaseState());
		dictionary.put("purchase_time", purchase.getPurchaseTime());
		dictionary.put("purchase_token", purchase.getPurchaseToken());
		dictionary.put("quantity", purchase.getQuantity());
		dictionary.put("signature", purchase.getSignature());
		// PBL V4 replaced getSku with getSkus to support multi-sku purchases,
		// use the first entry for "sku" and generate an array for "skus"
		ArrayList<String> skus = purchase.getSkus();
		dictionary.put("sku", skus.get(0));
		String[] skusArray = skus.toArray(new String[0]);
		dictionary.put("skus", skusArray);
		dictionary.put("is_acknowledged", purchase.isAcknowledged());
		dictionary.put("is_auto_renewing", purchase.isAutoRenewing());
		return dictionary;
	}

	public static Dictionary convertSkuDetailsToDictionary(SkuDetails details) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("sku", details.getSku());
		dictionary.put("title", details.getTitle());
		dictionary.put("description", details.getDescription());
		dictionary.put("price", details.getPrice());
		dictionary.put("price_currency_code", details.getPriceCurrencyCode());
		dictionary.put("price_amount_micros", details.getPriceAmountMicros());
		dictionary.put("free_trial_period", details.getFreeTrialPeriod());
		dictionary.put("icon_url", details.getIconUrl());
		dictionary.put("introductory_price", details.getIntroductoryPrice());
		dictionary.put("introductory_price_amount_micros", details.getIntroductoryPriceAmountMicros());
		dictionary.put("introductory_price_cycles", details.getIntroductoryPriceCycles());
		dictionary.put("introductory_price_period", details.getIntroductoryPricePeriod());
		dictionary.put("original_price", details.getOriginalPrice());
		dictionary.put("original_price_amount_micros", details.getOriginalPriceAmountMicros());
		dictionary.put("subscription_period", details.getSubscriptionPeriod());
		dictionary.put("type", details.getType());
		return dictionary;
	}

	public static Dictionary convertProductDetailsToDictionary(ProductDetails details) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("sku", details.getProductId());
		dictionary.put("title", details.getTitle());
		dictionary.put("description", details.getDescription());
		dictionary.put("price", null);
		dictionary.put("price_currency_code", null);
		dictionary.put("price_amount_micros", null);
		dictionary.put("free_trial_period", null);
		dictionary.put("icon_url", null);
		dictionary.put("introductory_price", null);
		dictionary.put("introductory_price_amount_micros", null);
		dictionary.put("introductory_price_cycles", null);
		dictionary.put("introductory_price_period", null);
		dictionary.put("original_price", null);
		dictionary.put("original_price_amount_micros", null);
		dictionary.put("subscription_period", null);
		dictionary.put("type", null);
		return dictionary;
	}


	public static Object[] convertPurchaseListToDictionaryObjectArray(List<Purchase> purchases) {
		Object[] purchaseDictionaries = new Object[purchases.size()];

		for (int i = 0; i < purchases.size(); i++) {
			purchaseDictionaries[i] = GooglePlayBillingUtils.convertPurchaseToDictionary(purchases.get(i));
		}

		return purchaseDictionaries;
	}

	public static Object[] convertSkuDetailsListToDictionaryObjectArray(List<SkuDetails> skuDetails) {
		Object[] skuDetailsDictionaries = new Object[skuDetails.size()];

		for (int i = 0; i < skuDetails.size(); i++) {
			skuDetailsDictionaries[i] = GooglePlayBillingUtils.convertSkuDetailsToDictionary(skuDetails.get(i));
		}

		return skuDetailsDictionaries;
	}

	public static Object[] convertProductDetailsListToDictionaryObjectArray(List<ProductDetails> productDetails) {
		Object[] skuDetailsDictionaries = new Object[productDetails.size()];

		for (int i = 0; i < productDetails.size(); i++) {
			skuDetailsDictionaries[i] = GooglePlayBillingUtils.convertProductDetailsToDictionary(productDetails.get(i));
		}

		return skuDetailsDictionaries;
	}

	public static void addProductDetailsByProductId(List<ProductDetails> allProductDetails, HashMap<String, ProductDetails> allProductDetailsByProductId) {
		if (allProductDetails == null) return;

		for (int i = 0; i < allProductDetails.size(); i++) {
			ProductDetails productDetails = allProductDetails.get(i);
			String productId = productDetails.getProductId();

			allProductDetailsByProductId.put(productId, productDetails);
		}
	}

	public static Dictionary convertFromBillingResult(BillingResult billingResult) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("debug_message", billingResult.getDebugMessage()); // String
		dictionary.put("response_code", billingResult.getResponseCode()); // int
		return dictionary;
	}

	public static Object[] convertFromProductDetailsArr(List<ProductDetails> allProductDetails) {
		if (allProductDetails == null) {
			return new Object[] {  };
		}

		Object[] allDictionaries = new Object[allProductDetails.size()];

		for (int i = 0; i < allDictionaries.length; i++) {
			allDictionaries[i] = convertFromProductDetails(allProductDetails.get(i));
		}

		return allDictionaries;
	}

	public static Dictionary convertFromProductDetails(ProductDetails productDetails) {
		Dictionary dictionary = new Dictionary();

		dictionary.put("description", productDetails.getDescription()); // String
		dictionary.put("name", productDetails.getName()); // String
		dictionary.put("one_time_purchase_offer_details", convertFromOneTimePurchaseOfferDetails(productDetails.getOneTimePurchaseOfferDetails())); // Dictionary
		dictionary.put("product_id", productDetails.getProductId()); // String
		dictionary.put("product_type", productDetails.getProductType()); // String
		dictionary.put("subscription_offer_details", convertFromSubscriptionOfferDetailsArr(productDetails.getSubscriptionOfferDetails())); // Godot Array of Dictionaries
		dictionary.put("title", productDetails.getTitle()); // String

		return dictionary;
	}

	public static Dictionary convertFromOneTimePurchaseOfferDetails(ProductDetails.OneTimePurchaseOfferDetails oneTimePurchaseOfferDetails) {
		Dictionary dictionary = new Dictionary();

		// Developer docs says it's possible for this to be null.
		if (oneTimePurchaseOfferDetails == null) {
			return dictionary;
		}

		dictionary.put("formatted_price", oneTimePurchaseOfferDetails.getFormattedPrice()); // String
		dictionary.put("price_amount_micros", oneTimePurchaseOfferDetails.getPriceAmountMicros()); // long
		dictionary.put("price_currency_code", oneTimePurchaseOfferDetails.getPriceCurrencyCode()); // String
		return dictionary;
	}

	public static Object[] convertFromSubscriptionOfferDetailsArr(List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails) {
		// Developer docs says it's possible for this to be null.
		if (subscriptionOfferDetails == null) {
			return new Object[] {  };
		}

		Object[] allDictionaries = new Object[subscriptionOfferDetails.size()];

		for (int i = 0; i < allDictionaries.length; i++) {
			allDictionaries[i] = convertFromSubscriptionOfferDetails(subscriptionOfferDetails.get(i));
		}

		return allDictionaries;
	}

	public static Dictionary convertFromSubscriptionOfferDetails(ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("base_plan_id", subscriptionOfferDetails.getBasePlanId()); // String
		dictionary.put("installment_plan_details", convertFromInstallmentPlanDetails(subscriptionOfferDetails.getInstallmentPlanDetails())); // Dictionary
		dictionary.put("offer_id", subscriptionOfferDetails.getOfferId()); // String
		dictionary.put("offer_tags", subscriptionOfferDetails.getOfferTags().toArray()); // String[]
		dictionary.put("offer_token", subscriptionOfferDetails.getOfferToken()); // String
		dictionary.put("pricing_phases", convertFromPricingPhases(subscriptionOfferDetails.getPricingPhases())); // Dictionary
		return dictionary;
	}

	public static Dictionary convertFromInstallmentPlanDetails(ProductDetails.InstallmentPlanDetails installmentPlanDetails) {
		Dictionary dictionary = new Dictionary();

		if (installmentPlanDetails == null) {
			return dictionary;
		}

		dictionary.put("installment_plan_commitment_payments_count", installmentPlanDetails.getInstallmentPlanCommitmentPaymentsCount()); // int
		dictionary.put("subsequent_installment_plan_commitment_payments_count", installmentPlanDetails.getSubsequentInstallmentPlanCommitmentPaymentsCount()); // int
		return dictionary;
	}

	public static Dictionary convertFromPricingPhases(ProductDetails.PricingPhases pricingPhases) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("pricing_phase_list", convertFromPricingPhaseArr(pricingPhases.getPricingPhaseList())); // Array of Dictionaries
		return dictionary;
	}
	public static Object[] convertFromPricingPhaseArr(List<ProductDetails.PricingPhase> allPricingPhases) {
		if (allPricingPhases == null) {
			return new Object[] {  };
		}

		Object[] allDictionaries = new Object[allPricingPhases.size()];

		for (int i = 0; i < allDictionaries.length; i++) {
			allDictionaries[i] = convertFromPricingPhase(allPricingPhases.get(i));
		}

		return allDictionaries;
	}
	public static Dictionary convertFromPricingPhase(ProductDetails.PricingPhase pricingPhase) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("billing_cycle_count", pricingPhase.getBillingCycleCount()); // int
		dictionary.put("billing_period", pricingPhase.getBillingPeriod()); // String
		dictionary.put("formatted_price", pricingPhase.getFormattedPrice()); // String
		dictionary.put("price_amount_micros", pricingPhase.getPriceAmountMicros()); // long
		dictionary.put("price_currency_code", pricingPhase.getPriceCurrencyCode()); // String
		dictionary.put("recurrence_mode", pricingPhase.getRecurrenceMode()); // int
		return dictionary;
	}

	public static void addPurchasesByPurchaseToken(List<Purchase> allPurchases, HashMap<String, Purchase> allPurchasesByPurchaseToken) {
		if (allPurchases == null) return;

		for (int i = 0; i < allPurchases.size(); i++) {
			Purchase purchase = allPurchases.get(i);
			String purchaseToken = purchase.getPurchaseToken();

			allPurchasesByPurchaseToken.put(purchaseToken, purchase);
		}
	}
	public static Object[] convertFromPurchaseArr(List<Purchase> allPurchases) {
		if (allPurchases == null) {
			return new Object[] {  };
		}

		Object[] allDictionaries = new Object[allPurchases.size()];

		for (int i = 0; i < allDictionaries.length; i++) {
			allDictionaries[i] = convertFromPurchase(allPurchases.get(i));
		}

		return allDictionaries;
	}
	public static Dictionary convertFromPurchase(Purchase purchase) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("account_identifiers", convertFromAccountIdentifiers(purchase.getAccountIdentifiers())); // Dictionary
		dictionary.put("developer_payload", purchase.getDeveloperPayload()); // String
		dictionary.put("order_id", purchase.getOrderId()); // String
		dictionary.put("original_json", purchase.getOriginalJson()); // String
		dictionary.put("package_name", purchase.getPackageName()); // String
		dictionary.put("pending_purchase_update", convertFromPendingPurchaseUpdate(purchase.getPendingPurchaseUpdate())); // Dictionary
		dictionary.put("products", purchase.getProducts().toArray()); // String[]
		dictionary.put("purchase_state", purchase.getPurchaseState()); // int
		dictionary.put("purchase_time", purchase.getPurchaseTime()); // long
		dictionary.put("purchase_token", purchase.getPurchaseToken()); // String
		dictionary.put("quantity", purchase.getQuantity()); // int
		dictionary.put("signature", purchase.getSignature()); // String
		dictionary.put("is_acknowledged", purchase.isAcknowledged()); // boolean
		dictionary.put("is_auto_renewing", purchase.isAutoRenewing()); // boolean
		return dictionary;
	}

	public static Dictionary convertFromAccountIdentifiers(AccountIdentifiers accountIdentifiers) {
		Dictionary dictionary = new Dictionary();

		if (accountIdentifiers == null) {
			return dictionary;
		}

		dictionary.put("obfuscated_account_id", accountIdentifiers.getObfuscatedAccountId()); // String
		dictionary.put("obfuscated_profile_id", accountIdentifiers.getObfuscatedProfileId()); // String
		return dictionary;
	}
	public static Dictionary convertFromPendingPurchaseUpdate(Purchase.PendingPurchaseUpdate pendingPurchaseUpdate) {
		Dictionary dictionary = new Dictionary();

		if (pendingPurchaseUpdate == null) {
			return dictionary;
		}

		dictionary.put("products", pendingPurchaseUpdate.getProducts().toArray()); // String[]
		dictionary.put("purchase_token", pendingPurchaseUpdate.getPurchaseToken()); // String
		return dictionary;
	}

}
