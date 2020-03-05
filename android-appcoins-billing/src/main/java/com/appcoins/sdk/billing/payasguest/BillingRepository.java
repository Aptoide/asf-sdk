package com.appcoins.sdk.billing.payasguest;

import com.appcoins.sdk.billing.service.RequestResponse;
import com.appcoins.sdk.billing.service.Service;
import com.appcoins.sdk.billing.service.ServiceResponseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingRepository {

  private Service service;

  public BillingRepository(Service service) {

    this.service = service;
  }

  public void getSkuPurchase(String packageName, String sku, String walletAddress,
      String walletSignature, final PurchaseListener purchaseListener) {
    ServiceResponseListener serviceResponseListener = new ServiceResponseListener() {
      @Override public void onResponseReceived(RequestResponse requestResponse) {
        PurchaseMapper purchaseMapper = new PurchaseMapper();
        PurchaseModel purchaseModel = purchaseMapper.map(requestResponse);
        purchaseListener.onResponse(purchaseModel);
      }
    };

    List<String> path = new ArrayList<>();
    path.add(packageName);
    path.add("products");
    path.add(sku);
    path.add("purchase");

    Map<String, String> queries = new HashMap<>();
    queries.put("wallet.address", walletAddress);
    queries.put("wallet.signature", walletSignature);
    service.makeRequest("/inapp/8.20180518/packages", "GET", path, queries, null, null,
        serviceResponseListener);
  }
}