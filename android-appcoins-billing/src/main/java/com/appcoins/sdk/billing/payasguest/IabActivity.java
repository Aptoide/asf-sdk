package com.appcoins.sdk.billing.payasguest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import com.appcoins.sdk.billing.BuyItemProperties;
import com.appcoins.sdk.billing.WebViewActivity;
import com.appcoins.sdk.billing.helpers.InstallDialogActivity;
import com.appcoins.sdk.billing.helpers.TranslationsModel;
import com.appcoins.sdk.billing.helpers.TranslationsXmlParser;
import com.appcoins.sdk.billing.helpers.Utils;
import com.appcoins.sdk.billing.listeners.payasguest.ActivityResultListener;
import java.util.Locale;

import static com.appcoins.sdk.billing.helpers.AppcoinsBillingStubHelper.BUY_ITEM_PROPERTIES;
import static com.appcoins.sdk.billing.helpers.InstallDialogActivity.ERROR_RESULT_CODE;
import static com.appcoins.sdk.billing.helpers.Utils.RESPONSE_CODE;
import static com.appcoins.sdk.billing.utils.LayoutUtils.generateRandomId;

public class IabActivity extends Activity implements IabView {

  public final static int LAUNCH_INSTALL_BILLING_FLOW_REQUEST_CODE = 10001;
  public final static String PAYMENT_METHOD_KEY = "payment_method";
  public final static String WALLET_ADDRESS_KEY = "wallet_address_key";
  public final static String EWT_KEY = "ewt_key";
  public final static String SIGNATURE_KEY = "signature_key";
  public final static String FIAT_VALUE_KEY = "fiat_value";
  public final static String FIAT_CURRENCY_KEY = "fiat_currency";
  public final static String APPC_VALUE_KEY = "appc_value";
  public final static String SKU_KEY = "sku_key";
  private final static String TRANSLATIONS = "translations";
  private final static int WEB_VIEW_REQUEST_CODE = 1234;
  private static int IAB_ACTIVITY_MAIN_LAYOUT_ID = 1;
  private TranslationsModel translationsModel;
  private FrameLayout frameLayout;
  private BuyItemProperties buyItemProperties;
  private ActivityResultListener activityResultListener;
  private boolean backEnabled = true;

  @SuppressLint("ResourceType") @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //This log is necessary for the automatic test that validates the wallet installation dialog
    Log.d("InstallDialog", "com.appcoins.sdk.billing.helpers.InstallDialogActivity started");

    int backgroundColor = Color.parseColor("#64000000");
    frameLayout = new FrameLayout(this);
    if (savedInstanceState == null) {
      IAB_ACTIVITY_MAIN_LAYOUT_ID = generateRandomId(IAB_ACTIVITY_MAIN_LAYOUT_ID);
    }
    frameLayout.setId(IAB_ACTIVITY_MAIN_LAYOUT_ID);
    frameLayout.setBackgroundColor(backgroundColor);

    setContentView(frameLayout);

    buyItemProperties = (BuyItemProperties) getIntent().getSerializableExtra(BUY_ITEM_PROPERTIES);

    if (savedInstanceState != null) {
      translationsModel = (TranslationsModel) savedInstanceState.get(TRANSLATIONS);
    } else {
      fetchTranslations();
      navigateToPaymentSelection();
    }
  }

  @Override protected void onDestroy() {
    unlockRotation();
    super.onDestroy();
  }

  @Override public void onBackPressed() {
    if (backEnabled) {
      close();
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == LAUNCH_INSTALL_BILLING_FLOW_REQUEST_CODE) {
      setResult(resultCode, data);
      finish();
    } else if (requestCode == WEB_VIEW_REQUEST_CODE) {
      if (resultCode == WebViewActivity.SUCCESS) {
        if (activityResultListener != null) {
          activityResultListener.onActivityResult(data.getData());
        } else {
          Log.w("IabActivity", "ActivityResultListener was not set");
          close();
        }
      } else {
        close();
      }
    }
  }

  private void fetchTranslations() {
    Locale locale = Locale.getDefault();
    if (translationsModel == null || !translationsModel.getLanguageCode()
        .equalsIgnoreCase(locale.getLanguage()) || !translationsModel.getCountryCode()
        .equalsIgnoreCase(locale.getCountry())) {
      TranslationsXmlParser translationsParser = new TranslationsXmlParser(this);
      translationsModel =
          translationsParser.parseTranslationXml(locale.getLanguage(), locale.getCountry());
    }
  }

  private void navigateTo(Fragment fragment) {
    getFragmentManager().beginTransaction()
        .replace(frameLayout.getId(), fragment)
        .commit();
  }

  @Override public TranslationsModel getTranslationsModel() {
    return translationsModel;
  }

  @Override public void close() {
    Bundle bundle = new Bundle();
    bundle.putInt(RESPONSE_CODE, 1); //CANCEL
    Intent intent = new Intent();
    intent.putExtras(bundle);
    setResult(Activity.RESULT_CANCELED, intent);
    finish();
  }

  @Override public void finishWithError() {
    Intent response = new Intent();
    response.putExtra("RESPONSE_CODE", ERROR_RESULT_CODE);
    setResult(ERROR_RESULT_CODE, response);
    finish();
  }

  @Override public void showAlertNoBrowserAndStores() {
    buildAlertNoBrowserAndStores();
  }

  @Override public void redirectToWalletInstallation(Intent intent) {
    startActivity(intent);
  }

  @Override
  public void navigateToAdyen(String selectedRadioButton, String walletAddress, String ewt,
      String signature, String fiatPrice, String fiatPriceCurrencyCode, String appcPrice,
      String sku) {
    AdyenPaymentFragment adyenPaymentFragment = new AdyenPaymentFragment();
    Bundle bundle = new Bundle();
    bundle.putString(PAYMENT_METHOD_KEY, selectedRadioButton);
    bundle.putString(WALLET_ADDRESS_KEY, walletAddress);
    bundle.putString(EWT_KEY, ewt);
    bundle.putString(SIGNATURE_KEY, signature);
    bundle.putString(FIAT_VALUE_KEY, fiatPrice);
    bundle.putString(FIAT_CURRENCY_KEY, fiatPriceCurrencyCode);
    bundle.putString(APPC_VALUE_KEY, appcPrice);
    bundle.putString(SKU_KEY, sku);
    bundle.putSerializable(BUY_ITEM_PROPERTIES, buyItemProperties);
    adyenPaymentFragment.setArguments(bundle);
    navigateTo(adyenPaymentFragment);
  }

  @Override public void startIntentSenderForResult(IntentSender intentSender, int requestCode) {
    try {
      startIntentSenderForResult(intentSender, requestCode, new Intent(), 0, 0, 0);
    } catch (IntentSender.SendIntentException e) {
      finishWithError();
    }
  }

  @Override public void lockRotation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    }
  }

  @Override public void unlockRotation() {
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
  }

  @Override public void navigateToUri(String url, ActivityResultListener activityResultListener) {
    this.activityResultListener = activityResultListener;
    startActivityForResult(WebViewActivity.newIntent(this, url), WEB_VIEW_REQUEST_CODE);
  }

  @Override public void finish(Bundle bundle) {
    setResult(Activity.RESULT_OK, new Intent().putExtras(bundle));
    finish();
  }

  @Override public void navigateToPaymentSelection() {
    navigateTo(PaymentMethodsFragment.newInstance(buyItemProperties));
  }

  @Override public void navigateToInstallDialog() {
    Intent intent = new Intent(this.getApplicationContext(), InstallDialogActivity.class);
    intent.putExtra(BUY_ITEM_PROPERTIES, buyItemProperties);
    finish();
    startActivity(intent);
  }

  @Override public void disableBack() {
    backEnabled = false;
  }

  private void buildAlertNoBrowserAndStores() {
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
    String value = translationsModel.getAlertDialogMessage();
    String dismissValue = translationsModel.getAlertDialogDismissButton();
    alert.setMessage(value);
    alert.setCancelable(true);
    alert.setPositiveButton(dismissValue, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        Bundle response = new Bundle();
        response.putInt(Utils.RESPONSE_CODE, 1);
        Intent intent = new Intent();
        intent.putExtras(response);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
      }
    });
    AlertDialog alertDialog = alert.create();
    alertDialog.show();
  }
}