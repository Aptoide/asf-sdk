package com.asf.appcoins.sdk.ads.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import com.asf.appcoins.sdk.ads.BuildConfig;
import com.asf.appcoins.sdk.ads.poa.manager.WalletUtils;
import java.util.List;

public class AppcoinsAdvertisementConnection implements ServiceConnection {

  private final ConnectionLifeCycle connectionLifeCycle;
  private Context context;
  private AppcoinsAdvertisementListener listener;

  public AppcoinsAdvertisementConnection(Context context, ConnectionLifeCycle connectionLifeCycle) {
    this.context = context;
    this.connectionLifeCycle = connectionLifeCycle;
  }

  @Override public void onServiceConnected(ComponentName name, IBinder service) {
    connectionLifeCycle.onConnect(service, listener);
  }

  @Override public void onServiceDisconnected(ComponentName name) {

  }

  public void startConnection(final AppcoinsAdvertisementListener appCoinsAdvertisementListener) {
    this.listener = appCoinsAdvertisementListener;
    Intent serviceIntent = new Intent(BuildConfig.ADVERTISEMENT_BIND_ACTION);

    serviceIntent.setPackage(WalletUtils.getBillingServicePackageName());

    List<ResolveInfo> intentServices = context.getPackageManager()
        .queryIntentServices(serviceIntent, 0);
    if (intentServices != null && !intentServices.isEmpty()) {
      context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    } else {
      appCoinsAdvertisementListener.onAdvertisementFinished(ResponseCode.ERROR.getValue());
    }
  }
}
