package com.appcoins.sdk.billing.helpers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.appcoins.billing.AppcoinsBilling;
import com.appcoins.billing.sdk.BuildConfig;
import com.appcoins.sdk.billing.BuyItemProperties;
import com.appcoins.sdk.billing.StartPurchaseAfterBindListener;
import com.appcoins.sdk.billing.ResponseCode;
import com.appcoins.sdk.billing.SkuDetails;
import com.appcoins.sdk.billing.SkuDetailsResult;
import com.appcoins.sdk.billing.WSServiceController;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class AppcoinsBillingStubHelper implements AppcoinsBilling, Serializable {
  private static final String TAG = AppcoinsBillingStubHelper.class.getSimpleName();

  private static AppcoinsBilling serviceAppcoinsBilling;

  private final static String APPCOINS_BILLING_STUB_HELPER_INSTANCE =
      "appcoins_billing_stub_helper";
  public final static String BUY_ITEM_PROPERTIES = "buy_item_properties";
  private static AppcoinsBillingStubHelper appcoinsBillingStubHelper;

  private AppcoinsBillingStubHelper() {
    this.appcoinsBillingStubHelper = this;
  }

  @Override public int isBillingSupported(int apiVersion, String packageName, String type) {

    if (WalletUtils.hasWalletInstalled()) {
      try {
        return serviceAppcoinsBilling.isBillingSupported(apiVersion, packageName, type);
      } catch (RemoteException e) {
        e.printStackTrace();
        return ResponseCode.SERVICE_UNAVAILABLE.getValue();
      }
    } else {
      if (type.equalsIgnoreCase("inapp")) {
        if (apiVersion == 3) {
          return ResponseCode.OK.getValue();
        } else {
          return ResponseCode.BILLING_UNAVAILABLE.getValue();
        }
      } else {
        return ResponseCode.BILLING_UNAVAILABLE.getValue();
      }
    }
  }

  @Override
  public Bundle getSkuDetails(final int apiVersion, final String packageName, final String type,
      final Bundle skusBundle) {

    Bundle responseWs = new Bundle();
    if (WalletUtils.hasWalletInstalled()) {
      try {
        return serviceAppcoinsBilling.getSkuDetails(apiVersion, packageName, type, skusBundle);
      } catch (RemoteException e) {
        e.printStackTrace();
        responseWs.putInt(Utils.RESPONSE_CODE, ResponseCode.SERVICE_UNAVAILABLE.getValue());
      }
    } else {
      List<String> sku = skusBundle.getStringArrayList(Utils.GET_SKU_DETAILS_ITEM_LIST);
      String response =
          WSServiceController.getSkuDetailsService(BuildConfig.HOST_WS, packageName, sku);
      responseWs.putInt(Utils.RESPONSE_CODE, 0);
      ArrayList<String> skuDetails = buildResponse(response, type);
      responseWs.putStringArrayList("DETAILS_LIST", skuDetails);
    }
    return responseWs;
  }

  private ArrayList<String> buildResponse(String response, String type) {
    SkuDetailsResult skuDetailsResult = AndroidBillingMapper.mapSkuDetailsFromWS(type, response);
    ArrayList<String> list = new ArrayList<>();
    for (SkuDetails skuDetails : skuDetailsResult.getSkuDetailsList()) {
      list.add(AndroidBillingMapper.mapSkuDetailsResponse(skuDetails));
    }
    return list;
  }

  @Override public Bundle getBuyIntent(int apiVersion, String packageName, String sku, String type,
      String developerPayload) {
    if (WalletUtils.hasWalletInstalled()) {
      try {
        return serviceAppcoinsBilling.getBuyIntent(apiVersion, packageName, sku, type,
            developerPayload);
      } catch (RemoteException e) {
        e.printStackTrace();
        Bundle response = new Bundle();
        response.putInt(Utils.RESPONSE_CODE, ResponseCode.SERVICE_UNAVAILABLE.getValue());
        return response;
      }
    } else {
      BuyItemProperties buyItemProperties =
          new BuyItemProperties(apiVersion, packageName, sku, type, developerPayload);

      Context context = WalletUtils.getContext();

      Intent intent = new Intent(context, InstallDialogActivity.class);
      intent.putExtra(APPCOINS_BILLING_STUB_HELPER_INSTANCE, this);
      intent.putExtra(BUY_ITEM_PROPERTIES, buyItemProperties);

      PendingIntent pendingIntent =
          PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      Bundle response = new Bundle();
      response.putParcelable("BUY_INTENT", pendingIntent);

      response.putInt(Utils.RESPONSE_CODE, ResponseCode.OK.getValue());
      return response;
    }
  }

  @Override public Bundle getPurchases(int apiVersion, String packageName, String type,
      String continuationToken) {
    Bundle bundleResponse = new Bundle();
    if (WalletUtils.hasWalletInstalled()) {
      try {
        return serviceAppcoinsBilling.getPurchases(apiVersion, packageName, type, null);
      } catch (RemoteException e) {
        e.printStackTrace();
        bundleResponse.putInt(Utils.RESPONSE_CODE, ResponseCode.SERVICE_UNAVAILABLE.getValue());
      }
    } else {

      bundleResponse.putInt(Utils.RESPONSE_CODE, ResponseCode.OK.getValue());
      bundleResponse.putStringArrayList("INAPP_PURCHASE_ITEM_LIST", new ArrayList<String>());
      bundleResponse.putStringArrayList("INAPP_PURCHASE_DATA_LIST", new ArrayList<String>());
      bundleResponse.putStringArrayList("INAPP_DATA_SIGNATURE_LIST", new ArrayList<String>());
    }

    return bundleResponse;
  }

  @Override public int consumePurchase(int apiVersion, String packageName, String purchaseToken) {
    if (WalletUtils.hasWalletInstalled()) {
      try {
        return serviceAppcoinsBilling.consumePurchase(apiVersion, packageName, purchaseToken);
      } catch (RemoteException e) {
        e.printStackTrace();
        return ResponseCode.SERVICE_UNAVAILABLE.getValue();
      }
    } else {
      return ResponseCode.OK.getValue();
    }
  }

  @Override public IBinder asBinder() {
    return null;
  }

  boolean createRepository(final StartPurchaseAfterBindListener startPurchaseAfterConnectionListenner) {

    String packageName = WalletUtils.getBillingServicePackageName();

    Intent serviceIntent = new Intent(BuildConfig.IAB_BIND_ACTION);
    serviceIntent.setPackage(BuildConfig.IAB_BIND_PACKAGE);
    serviceIntent.setPackage(packageName);

    final Context context = WalletUtils.getContext();

    List<ResolveInfo> intentServices = context.getPackageManager()
        .queryIntentServices(serviceIntent, 0);
    if (intentServices != null && !intentServices.isEmpty()) {
      return context.bindService(serviceIntent, new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
          serviceAppcoinsBilling = Stub.asInterface(service);
          startPurchaseAfterConnectionListenner.startPurchaseAfterBind();
          Log.d(TAG, "onServiceConnected() called service = [" + serviceAppcoinsBilling + "]");
        }

        @Override public void onServiceDisconnected(ComponentName name) {
          Log.d(TAG, "onServiceDisconnected() called = [" + name + "]");
        }
      }, Context.BIND_AUTO_CREATE);
    }
    return false;
  }

  static AppcoinsBillingStubHelper getInstance() {
    if (appcoinsBillingStubHelper == null) {
      appcoinsBillingStubHelper = new AppcoinsBillingStubHelper();
    }
    return appcoinsBillingStubHelper;
  }

  public static abstract class Stub {

    public static AppcoinsBilling asInterface(IBinder service) {
      if (!WalletUtils.hasWalletInstalled()) {
        return AppcoinsBillingStubHelper.getInstance();
      } else {
        return AppcoinsBilling.Stub.asInterface(service);
      }
    }
  }
}