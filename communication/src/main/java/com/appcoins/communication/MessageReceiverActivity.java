package com.appcoins.communication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

public abstract class MessageReceiverActivity extends Activity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    long messageId = intent.getLongExtra("MESSAGE_ID", -1);
    int methodId = intent.getIntExtra("TYPE", -1);
    Parcelable arguments = intent.getParcelableExtra("ARGUMENTS");
    onReceived(messageId, methodId, arguments);
  }

  public abstract void onReceived(long messageId, int methodId, Parcelable arguments);
}
