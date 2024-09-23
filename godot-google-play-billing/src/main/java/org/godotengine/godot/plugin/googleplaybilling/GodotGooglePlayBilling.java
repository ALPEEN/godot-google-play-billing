/*************************************************************************/
/*  GodotGooglePlayBilling.java                                                    */
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

package org.godotengine.godot.plugin.googleplaybilling;

import android.app.Activity;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.googleplaybilling.utils.GooglePlayBillingUtils;
import org.godotengine.godot.plugin.UsedByGodot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GodotGooglePlayBilling extends GodotPlugin {

	private final BillingClient billingClient;
	private final HashMap<String, ProductDetails> productDetailsCache = new HashMap<>(); // sku → SkuDetails
	private String obfuscatedAccountId;
	private String obfuscatedProfileId;
	private boolean isPurchasePersonalized;
	private final Activity mainActivity;
	private boolean billingClientAvailable;
	private final HashMap<String, ProductDetails> queriedProductDetailsByProductId = new HashMap<>();
	private final HashMap<String, Purchase> queriedPurchasesByPurchaseToken = new HashMap<>();

	private final String BILLING_SERVICE_DISCONNECTED = "billing_service_disconnected";
	private final String BILLING_SETUP_FINISHED = "billing_setup_finished";
	private final String PRODUCT_DETAILS_QUERY_COMPLETED = "product_details_query_completed";
	private final String QUERY_PURCHASES_RESPONSE = "query_purchases_response";
	private final String PURCHASES_UPDATED = "purchases_updated";
	private final String ACKNOWLEDGE_PURCHASE_RESPONSE = "acknowledge_purchase_response";
	private final String CONSUME_RESPONSE = "consume_response";
	private final String BILLING_RESUMED = "billing_resumed";


	public GodotGooglePlayBilling(Godot godot) {
		super(godot);
		mainActivity = godot.getActivity();

		if (mainActivity == null) {
			System.out.println("Godot Activity is null");
			billingClient = null;
			return;
		}

		PendingPurchasesParams pendingPurchasesParams = 
			PendingPurchasesParams.newBuilder().enableOneTimeProducts().build();

		billingClient = BillingClient.newBuilder(mainActivity)
				.enablePendingPurchases(pendingPurchasesParams)
				.setListener(purchasesUpdatedListener)
				.build();

		billingClientAvailable = false;
		isPurchasePersonalized = false;
		obfuscatedAccountId = "";
		obfuscatedProfileId = "";
	}

	@UsedByGodot
	public void startConnection() {
		billingClient.startConnection(billingClientStateListener);
	}
	@UsedByGodot
	public void endConnection() {
		billingClient.endConnection();
	}
	@UsedByGodot
	public boolean isReady() {
		return billingClient.isReady();
	}
	@UsedByGodot
	public boolean getBillingClientAvailable() {
		return billingClientAvailable;
	}
	@UsedByGodot
	public int getConnectionState() {
		return billingClient.getConnectionState();
	}

	private final BillingClientStateListener billingClientStateListener = new BillingClientStateListener() {

		@Override
		public void onBillingServiceDisconnected() {
			billingClientAvailable = false;
			emitSignal(BILLING_SERVICE_DISCONNECTED);
		}
		@Override
		public void onBillingSetupFinished(BillingResult billingResult) {
			if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
				billingClientAvailable = true;
			} else {
				billingClientAvailable = false;
			}

			emitSignal(BILLING_SETUP_FINISHED, GooglePlayBillingUtils.convertFromBillingResult(billingResult));
		}
	};
	@UsedByGodot
	public void queryPurchases(String productType) {
		// inapp or subs
		QueryPurchasesParams queryPurchasesParams =
				QueryPurchasesParams.newBuilder()
						.setProductType(productType)
						.build();

		billingClient.queryPurchasesAsync(queryPurchasesParams,
				new PurchasesResponseListener() {
					public void onQueryPurchasesResponse(BillingResult billingResult, List<Purchase> purchases) {
						GooglePlayBillingUtils.addPurchasesByPurchaseToken(purchases, queriedPurchasesByPurchaseToken);
						emitSignal(QUERY_PURCHASES_RESPONSE, GooglePlayBillingUtils.convertFromBillingResult(billingResult), (Object)GooglePlayBillingUtils.convertFromPurchaseArr(purchases));
					}
				});
	}

	private List<QueryProductDetailsParams.Product> buildProductList(List<String> productIds, @BillingClient.ProductType String productType) {
		List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
		for (String productId : productIds) {
			productList.add(QueryProductDetailsParams.Product.newBuilder()
					.setProductId(productId)
					.setProductType(productType)
					.build());
		}
		return productList;
	}

	@UsedByGodot
	public void setObfuscatedAccountId(String accountId) {
		obfuscatedAccountId = accountId;
	}
	@UsedByGodot
	public void setObfuscatedProfileId(String profileId) {
		obfuscatedProfileId = profileId;
	}

	@Override
	public void onMainResume() {
		if (billingClientAvailable) {
			emitSignal(BILLING_RESUMED);
		}
	}

	@UsedByGodot
	public void queryProductDetails(String[] allProductIds, String[] allProductTypes) {
		List<QueryProductDetailsParams.Product> productList = new ArrayList<>();

		for (int i = 0; i < allProductIds.length; i++) {
			if (i >= allProductTypes.length) {
				System.out.printf("queryProductDetails>i: %s exceeded productTypes count%n", i);
				continue;
			}

			String productId = allProductIds[i];
			String productType = allProductTypes[i];

			QueryProductDetailsParams.Product newProduct =
					QueryProductDetailsParams.Product.newBuilder()
							.setProductId(productId)
							.setProductType(productType)
							.build();
			productList.add(newProduct);
		}

		QueryProductDetailsParams queryProductDetailsParams =
				QueryProductDetailsParams.newBuilder()
						.setProductList(productList)
						.build();

		billingClient.queryProductDetailsAsync(
				queryProductDetailsParams,
				new ProductDetailsResponseListener() {
					public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> productDetailsList) {
						if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
							GooglePlayBillingUtils.addProductDetailsByProductId(productDetailsList, queriedProductDetailsByProductId);
						}

						emitSignal(PRODUCT_DETAILS_QUERY_COMPLETED, GooglePlayBillingUtils.convertFromBillingResult(billingResult), (Object)GooglePlayBillingUtils.convertFromProductDetailsArr(productDetailsList));
					}
				}
		);
	}


	@UsedByGodot
	public void acknowledgePurchase(final String purchaseToken) {
		AcknowledgePurchaseParams acknowledgePurchaseParams =
				AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchaseToken)
						.build();
		billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
			@Override
			public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
				emitSignal(ACKNOWLEDGE_PURCHASE_RESPONSE, GooglePlayBillingUtils.convertFromBillingResult(billingResult), purchaseToken);
			}
		});
	}
	@UsedByGodot
	public void consumePurchase(String purchaseToken) {
		ConsumeParams consumeParams = ConsumeParams.newBuilder()
				.setPurchaseToken(purchaseToken)
				.build();

		billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
			@Override
			public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
				emitSignal(CONSUME_RESPONSE, GooglePlayBillingUtils.convertFromBillingResult(billingResult), purchaseToken);
			}
		});
	}
	@UsedByGodot
	public Dictionary purchaseNonConsumable(String productId) {
		if (!queriedProductDetailsByProductId.containsKey(productId)) {
			System.out.println("purchaseNonConsumable>either purchases haven't been queried or no matching productId could be found");
			return new Dictionary();
		}

		ProductDetails selectedProductDetails = queriedProductDetailsByProductId.get(productId);
		BillingResult billingResult = launchPurchaseFlowOneTimePurchase(selectedProductDetails);
		return GooglePlayBillingUtils.convertFromBillingResult(billingResult);
	}
	@UsedByGodot
	public Dictionary purchaseConsumable(String[] allProductIds) {
		List<ProductDetails> allProductDetails = new ArrayList<>();

		for (int i = 0; i < allProductIds.length; i++) {
			String foundProductId = allProductIds[i];

			if (!queriedProductDetailsByProductId.containsKey(foundProductId)) {
				System.out.println("purchaseConsumable>either purchases haven't been queried or no matching productId could be found");
				return new Dictionary();
			}

			ProductDetails selectedProductDetails = queriedProductDetailsByProductId.get(foundProductId);
			allProductDetails.add(selectedProductDetails);
		}

		BillingResult billingResult = launchPurchaseFlowConsumablePurchase(allProductDetails);
		return GooglePlayBillingUtils.convertFromBillingResult(billingResult);
	}
	@UsedByGodot
	public Dictionary purchaseSubscription(String productId, String planId) {
		if (!queriedProductDetailsByProductId.containsKey(productId)) {
			System.out.println("purchaseSubscription>either purchases haven't been queried or no matching productId could be found");
			return new Dictionary();
		}

		ProductDetails productDetails = queriedProductDetailsByProductId.get(productId);
		List<ProductDetails.SubscriptionOfferDetails> subscriptionDetails = productDetails.getSubscriptionOfferDetails();

		if (subscriptionDetails == null) {
			System.out.println("purchaseSubscription>it appears the selected product ID is not a subscription product.");
			return new Dictionary();
		}

		ProductDetails.SubscriptionOfferDetails selectedOffer = null;

		for (int i = 0; i < subscriptionDetails.size(); i++) {
			ProductDetails.SubscriptionOfferDetails foundOffer = subscriptionDetails.get(i);

			if (foundOffer.getBasePlanId().equals(planId)) {
				selectedOffer = foundOffer;
				break;
			}
		}

		if (selectedOffer == null) {
			System.out.println("purchaseSubscription>could not find the correct subscription offer with provided planId.");
			return new Dictionary();
		}

		BillingResult billingResult = launchPurchaseFlowSubscription(productDetails, selectedOffer, null);
		return GooglePlayBillingUtils.convertFromBillingResult(billingResult);
	}

	private final PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
		@Override
		public void onPurchasesUpdated(final BillingResult billingResult, @Nullable final List<Purchase> purchases) {
			GooglePlayBillingUtils.addPurchasesByPurchaseToken(purchases, queriedPurchasesByPurchaseToken);
			emitSignal(PURCHASES_UPDATED, GooglePlayBillingUtils.convertFromBillingResult(billingResult), (Object)GooglePlayBillingUtils.convertFromPurchaseArr(purchases));
		}
	};
	private BillingResult launchPurchaseFlowOneTimePurchase(ProductDetails selectedOneTimePurchaseDetails) {
		List<BillingFlowParams.ProductDetailsParams> allProductDetailsParams = new ArrayList<>();

		BillingFlowParams.ProductDetailsParams productDetailsParams =
				BillingFlowParams.ProductDetailsParams.newBuilder()
						.setProductDetails(selectedOneTimePurchaseDetails)
						.build();
		allProductDetailsParams.add(productDetailsParams);

		BillingFlowParams billingFlowParams = CreateBillingFlow(allProductDetailsParams, null);

		return billingClient.launchBillingFlow(mainActivity, billingFlowParams);
	}
	private BillingResult launchPurchaseFlowSubscription(ProductDetails selectedSubscriptionDetails, ProductDetails.SubscriptionOfferDetails selectedSubscription, BillingFlowParams.SubscriptionUpdateParams subscriptionUpdateParams) {
		List<BillingFlowParams.ProductDetailsParams> allProductDetailsParams = new ArrayList<>();

		BillingFlowParams.ProductDetailsParams productDetailsParams =
				BillingFlowParams.ProductDetailsParams.newBuilder()
						.setProductDetails(selectedSubscriptionDetails)
						.setOfferToken(selectedSubscription.getOfferToken())
						.build();
		allProductDetailsParams.add(productDetailsParams);

		BillingFlowParams billingFlowParams = CreateBillingFlow(allProductDetailsParams, subscriptionUpdateParams);
		return billingClient.launchBillingFlow(mainActivity, billingFlowParams);
	}
	private BillingResult launchPurchaseFlowConsumablePurchase(List<ProductDetails> allSelectedConsumablePurchaseDetails) {
		List<BillingFlowParams.ProductDetailsParams> allProductDetailsParams = new ArrayList<>();

		for (int i = 0; i < allSelectedConsumablePurchaseDetails.size(); i++) {
			ProductDetails currentProductDetails = allSelectedConsumablePurchaseDetails.get(i);
			BillingFlowParams.ProductDetailsParams productDetailsParams =
					BillingFlowParams.ProductDetailsParams.newBuilder()
							.setProductDetails(currentProductDetails)
							.build();
			allProductDetailsParams.add(productDetailsParams);
		}

		BillingFlowParams billingFlowParams = CreateBillingFlow(allProductDetailsParams, null);
		return billingClient.launchBillingFlow(mainActivity, billingFlowParams);
	}

	@UsedByGodot
	public Dictionary updateSubscription(String productId, String planId, String oldPurchaseToken, String externalTransactionId, int subscriptionReplacementMode) {
		if (!queriedProductDetailsByProductId.containsKey(productId)) {
			System.out.println("purchaseSubscription>either purchases haven't been queried or no matching productId could be found");
			return new Dictionary();
		}

		ProductDetails productDetails = queriedProductDetailsByProductId.get(productId);
		List<ProductDetails.SubscriptionOfferDetails> subscriptionDetails = productDetails.getSubscriptionOfferDetails();

		if (subscriptionDetails == null) {
			System.out.println("purchaseSubscription>it appears the selected product ID is not a subscription product.");
			return new Dictionary();
		}

		ProductDetails.SubscriptionOfferDetails selectedOffer = null;

		for (int i = 0; i < subscriptionDetails.size(); i++) {
			ProductDetails.SubscriptionOfferDetails foundOffer = subscriptionDetails.get(i);

			if (foundOffer.getBasePlanId().equals(planId)) {
				selectedOffer = foundOffer;
				break;
			}
		}

		if (selectedOffer == null) {
			System.out.println("purchaseSubscription>could not find the correct subscription offer with provided planId.");
			return new Dictionary();
		}

		BillingFlowParams.SubscriptionUpdateParams subscriptionUpdateParams;

		if (externalTransactionId == null || externalTransactionId.isEmpty()) {
			subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
					.setOldPurchaseToken(oldPurchaseToken)
					.setSubscriptionReplacementMode(subscriptionReplacementMode)
					.build();
		} else {
			subscriptionUpdateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
					.setOldPurchaseToken(oldPurchaseToken)
					.setOriginalExternalTransactionId(externalTransactionId)
					.setSubscriptionReplacementMode(subscriptionReplacementMode)
					.build();
		}

		BillingResult billingResult = launchPurchaseFlowSubscription(productDetails, selectedOffer, subscriptionUpdateParams);
		return GooglePlayBillingUtils.convertFromBillingResult(billingResult);
	}

	private BillingFlowParams CreateBillingFlow(List<BillingFlowParams.ProductDetailsParams> allProductDetailsParams, BillingFlowParams.SubscriptionUpdateParams subscriptionUpdateParams) {
		BillingFlowParams.Builder billingFlowParamsBuilder = BillingFlowParams.newBuilder();
		billingFlowParamsBuilder.setProductDetailsParamsList(allProductDetailsParams);

		if (subscriptionUpdateParams != null) {
			billingFlowParamsBuilder.setSubscriptionUpdateParams(subscriptionUpdateParams);
		}
		if (!obfuscatedAccountId.isEmpty()) {
			billingFlowParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId);
		}
		if (!obfuscatedProfileId.isEmpty()) {
			billingFlowParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId);
		}
		if (isPurchasePersonalized) {
			billingFlowParamsBuilder.setIsOfferPersonalized(true);
		}
		return billingFlowParamsBuilder.build();
	}


	@NonNull
	@Override
	public String getPluginName() {
		return "GodotGooglePlayBilling";
	}

	@NonNull
	@Override
	public Set<SignalInfo> getPluginSignals() {
		Set<SignalInfo> signals = new ArraySet<>();
		signals.add(new SignalInfo(BILLING_SERVICE_DISCONNECTED));
		signals.add(new SignalInfo(BILLING_SETUP_FINISHED, Object.class)); // BillingResult
		signals.add(new SignalInfo(PRODUCT_DETAILS_QUERY_COMPLETED, Object.class, Object[].class)); // BillingResult, ProductDetails[]
		signals.add(new SignalInfo(QUERY_PURCHASES_RESPONSE, Object.class, Object[].class)); // BillingResult, Purchase[]
		signals.add(new SignalInfo(PURCHASES_UPDATED, Object.class, Object[].class)); // BillingResult, Purchase[]
		signals.add(new SignalInfo(ACKNOWLEDGE_PURCHASE_RESPONSE, Object.class, String.class)); // BillingResult, purchaseToken
		signals.add(new SignalInfo(CONSUME_RESPONSE, Object.class, String.class)); // BillingResult, purchaseToken
		signals.add(new SignalInfo(BILLING_RESUMED));
		return signals;
	}

}
