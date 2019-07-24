package com.asf.appcoins.sdk.ads.network;

import com.asf.appcoins.sdk.ads.network.clients.CampaignService;
import com.asf.appcoins.sdk.ads.network.clients.CheckConnectionCampaignService;
import com.asf.appcoins.sdk.ads.network.responses.CampaignResponseHandler;
import com.asf.appcoins.sdk.ads.network.responses.ClientResponseHandler;
import com.asf.appcoins.sdk.ads.network.responses.ConnectivityResponseHandler;

public class AppCoinsClient {

  private final String packageName;
  private final int versionCode;
  private final Interceptor interceptor;
  private final String serviceUrl;

  public AppCoinsClient(String packageName, int versionCode, String serviceUrl,
      Interceptor interceptor) {
    this.packageName = packageName;
    this.versionCode = versionCode;
    this.serviceUrl = serviceUrl;
    this.interceptor = interceptor;
  }

  public void getCampaign(QueryParams queryParams, ClientResponseHandler clientResponseHandler) {
    CampaignResponseHandler campaignResponseHandler =
        new CampaignResponseHandler(clientResponseHandler);
    CampaignService appcoinsHTTPClient =
        new CampaignService(packageName, versionCode, serviceUrl, interceptor, queryParams,
            campaignResponseHandler);

    Thread operation = new Thread(appcoinsHTTPClient);
    operation.start();
  }

  public void checkConnectivity(ClientResponseHandler clientResponseHandler) {
    ConnectivityResponseHandler connectivityResponseHandler =
        new ConnectivityResponseHandler(clientResponseHandler);
    CampaignService appcoinsHTTPClient =
        new CheckConnectionCampaignService(packageName, versionCode, serviceUrl, interceptor,
            connectivityResponseHandler);

    Thread operation = new Thread(appcoinsHTTPClient);
    operation.start();
  }
}
