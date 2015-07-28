package maia.jose.openNetworkMeasurer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.GoogleApiAvailability;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import io.fabric.sdk.android.Fabric;
import maia.jose.openNetworkMeasurer.cell.CellularMeasurementActivity;
import maia.jose.openNetworkMeasurer.wifi.WifiMeasurementActivity;


public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        final Context mContext = this.getApplicationContext();

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance(); //singleton
        googleApi.showErrorDialogFragment(this, googleApi.isGooglePlayServicesAvailable(this), 0);

        Button btn_measureCell = (Button) findViewById(R.id.button_measure_cellular);
        btn_measureCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), CellularMeasurementActivity.class);
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getPhoneType()==TelephonyManager.PHONE_TYPE_SIP ||
                        tm.getPhoneType()==TelephonyManager.PHONE_TYPE_NONE ||
                        tm.getSimState()==TelephonyManager.SIM_STATE_ABSENT ||
                        tm.getSimState()==TelephonyManager.SIM_STATE_UNKNOWN)
                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this).text("Device has no telephony!")
                                    .actionLabel("close")
                                    .actionColor(Color.WHITE));
                else
                    startActivity(i);
            }
        });

        Button btn_measureWifi = (Button) findViewById(R.id.button_measure_wifi);
        btn_measureWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), WifiMeasurementActivity.class);
                startActivity(i);
            }
        });

        Button btn_seeMeasurements = (Button) findViewById(R.id.button_see_measurements);
         btn_seeMeasurements.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), PreviousMeasurementActivity.class);
                if (AuxiliaryMethods.hasPreviousMeasurements())
                    startActivity(i);
                else
                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this).text("No previous measurements!")
                                    .actionLabel("close")
                                    .actionColor(Color.WHITE));
            }
        });

        Button btn_speedtest = (Button) findViewById(R.id.button_speedtest);
        btn_speedtest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), SpeedTestActivity.class);
                startActivity(i);
            }
        });


        Button btn_serversync = (Button) findViewById(R.id.button_serversync);
        btn_serversync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ServerSyncActivity.class);
                if (AuxiliaryMethods.hasPreviousMeasurements())
                    startActivity(i);
                else
                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this).text("No previous measurements!")
                                    .actionLabel("close")
                                    .actionColor(Color.WHITE));
            }
        });

        Button btn_viewmap = (Button) findViewById(R.id.button_viewmap);
        btn_viewmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ViewMapActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
