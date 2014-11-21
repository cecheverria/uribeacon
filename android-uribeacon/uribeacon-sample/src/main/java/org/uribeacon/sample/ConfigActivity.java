/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uribeacon.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.uribeacon.beacon.ConfigUriBeacon;
import org.uribeacon.config.ProtocolV1;
import org.uribeacon.config.ProtocolV2;
import org.uribeacon.config.UriBeaconConfig;
import org.uribeacon.config.UriBeaconConfig.UriBeaconCallback;
import org.uribeacon.scan.compat.ScanResult;

import java.net.URISyntaxException;
import java.util.List;

public class ConfigActivity extends Activity{
  private EditText mUriValue;
  private EditText mFlagsValue;
  private EditText mTxCal1;
  private EditText mTxCal2;
  private EditText mTxCal3;
  private EditText mTxCal4;
  private Spinner mTxPowerMode;
  private EditText mPeriod;

  private ProgressDialog mConnectionDialog = null;
  private static final byte DEFAULT_TX_POWER = -63;
  private final String TAG = "ConfigActivity";
  private UriBeaconConfig mUriBeaconConfig;

  private final UriBeaconCallback mUriBeaconCallback = new UriBeaconCallback() {
    @Override
    public void onUriBeaconRead(ConfigUriBeacon configUriBeacon, int status) {
      checkRequest(status);
      updateInputFields(configUriBeacon);
    }

    @Override
    public void onUriBeaconWrite(int status) {
      checkRequest(status);
      mUriBeaconConfig.closeUriBeacon();
      finish();
    }

    private void checkRequest(int status) {
      if (status == BluetoothGatt.GATT_FAILURE) {
        Toast.makeText(ConfigActivity.this, "Failed to update the beacon", Toast.LENGTH_SHORT)
            .show();
        finish();
      }
    }
  };



  public void saveConfigBeacon(MenuItem menu) {
    // block UI
    mUriValue.setEnabled(false);
    String uri = mUriValue.getText().toString();
    try {
      ConfigUriBeacon configUriBeacon = new ConfigUriBeacon.Builder()
          .uriString(uri)
          .txPowerLevel(DEFAULT_TX_POWER)
          .build();
      mUriBeaconConfig.writeUriBeacon(configUriBeacon);
    } catch (URISyntaxException e) {
      Toast.makeText(ConfigActivity.this, "Invalid Uri", Toast.LENGTH_LONG).show();
      mUriBeaconConfig.closeUriBeacon();
      finish();
    }
  }
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // set content view
    setContentView(R.layout.activity_configbeacon);

    initializeTextFields();

    // Get the device to connect to that was passed to us by the scan
    // results Activity.
    Intent intent = getIntent();
    if (intent.getExtras() != null) {
      ScanResult scanResult = intent.getExtras().getParcelable(ScanResult.class.getCanonicalName());
      BluetoothDevice device = scanResult.getDevice();
      if (device != null) {
        // start connection progress
        mConnectionDialog = new ProgressDialog(this);
        mConnectionDialog.setIndeterminate(true);
        mConnectionDialog.setMessage("Connecting to device...");
        mConnectionDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
      }
      List<ParcelUuid> uuids = scanResult.getScanRecord().getServiceUuids();
      // Assuming the first uuid is the config uuid
      ParcelUuid uuid = uuids.get(0);
      mUriBeaconConfig = new UriBeaconConfig(this, mUriBeaconCallback, uuid);
      mUriBeaconConfig.connectUriBeacon(device);
    }
  }

  private void initializeTextFields() {
    mUriValue = (EditText) findViewById(R.id.editText_uri);
    mUriValue.addTextChangedListener(textWatcherFactory(R.id.uriLabel, mUriValue));
    mFlagsValue  = (EditText) findViewById(R.id.editText_flags);
    mFlagsValue.addTextChangedListener(textWatcherFactory(R.id.flagsLabel, mFlagsValue));
    mTxCal1  = (EditText) findViewById(R.id.editText_txCal1);
    mTxCal2 = (EditText) findViewById(R.id.editText_txCal2);
    mTxCal3 = (EditText) findViewById(R.id.editText_txCal3);
    mTxCal4 = (EditText) findViewById(R.id.editText_txCal4);
    //mTxPowerMode = (Spinner) findViewById(R.id.spinner_powerMode);
    mPeriod = (EditText) findViewById(R.id.editText_beaconPeriod);
    mPeriod.addTextChangedListener(textWatcherFactory(R.id.periodLabel, mPeriod));
  }


  public TextWatcher textWatcherFactory(final int labelId, final EditText editText) {
   return new TextWatcher() {
     @Override
     public void beforeTextChanged(CharSequence s, int start, int count, int after) {

     }

     @Override
     public void onTextChanged(CharSequence s, int start, int before, int count) {

     }

     @Override
     public void afterTextChanged(Editable s) {
       TextView label = (TextView) findViewById(labelId);
       if (editText.getText().toString().isEmpty()) {
         label.setTextColor(Color.WHITE);
       }
       else {
         label.setTextColor(Color.BLACK);
       }
     }
   };
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.config_menu, menu);
    return true;
  }
  public static void startConfigureActivity(Context context, ScanResult scanResult) {
    Intent intent = new Intent(context, ConfigActivity.class);
    intent.putExtra(ScanResult.class.getCanonicalName(), scanResult);
    context.startActivity(intent);
  }

  private void updateInputFields(ConfigUriBeacon configUriBeacon) {
    if (mUriValue != null && configUriBeacon != null) {
      mUriValue.setText(configUriBeacon.getUriString());
      if (mUriBeaconConfig.getVersion().equals(ProtocolV2.CONFIG_SERVICE_UUID)) {
        //TODO(g-ortuno): Set the rest of the characteristics for V2
        mFlagsValue.setText(byteToHexString(configUriBeacon.getFlags()));
      }
      else if (mUriBeaconConfig.getVersion().equals(ProtocolV1.CONFIG_SERVICE_UUID)) {
        hideV2Fields();
      }
    }
  }
  private void hideV2Fields(){
    mFlagsValue.setVisibility(View.GONE);
    mTxCal1.setVisibility(View.GONE);
    mTxCal2.setVisibility(View.GONE);
    mTxCal3.setVisibility(View.GONE);
    mTxCal4.setVisibility(View.GONE);
    //mTxPowerMode.setVisibility(View.GONE);
    mPeriod.setVisibility(View.GONE);
    findViewById(R.id.advertisedTxPowerLabel).setVisibility(View.GONE);
    findViewById(R.id.txPowerMode).setVisibility(View.GONE);
    findViewById(R.id.spinner_powerMode).setVisibility(View.GONE);
    findViewById(R.id.button_resetBeacon).setVisibility(View.GONE);

  }
  private String byteToHexString(byte theByte) {
    return String.format("%02X", theByte);
  }
  @Override
  public void onDestroy() {
    // Close and release Bluetooth connection.
    mUriBeaconConfig.closeUriBeacon();
    Toast.makeText(this, "Disconnected from beacon", Toast.LENGTH_SHORT).show();
    super.onDestroy();
  }
}
