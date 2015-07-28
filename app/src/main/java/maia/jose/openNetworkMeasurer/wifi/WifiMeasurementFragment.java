package maia.jose.openNetworkMeasurer.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import maia.jose.openNetworkMeasurer.AuxiliaryMethods;
import maia.jose.openNetworkMeasurer.GenericInfoTask;
import maia.jose.openNetworkMeasurer.GenericResults;
import maia.jose.openNetworkMeasurer.PressureListener;
import maia.jose.openNetworkMeasurer.R;
import maia.jose.openNetworkMeasurer.layout.MeasurementArrayAdapter;

public class WifiMeasurementFragment extends Fragment {

    private final String LOG_TAG = WifiMeasurementFragment.class.getSimpleName();

    public MeasurementArrayAdapter measurementAdapter;
    private WifiResults wifiStuff;
    private WifiManager wm;
    private BroadcastReceiver wifiReceiver;
    private Context myContext;
    private String currBSSID;
    private WifiInfo currentInfo;
    private String genericJSON;
    private String wifiJSON;
    private SensorManager mSensorManager = null;
    private PressureListener pressureListener;
    private GenericResults genericResults;
    private Location mLastLocation;
    private boolean measurementComplete = false;
    private Sensor pressureSensor;
    private boolean isInstantWifi = true;
    private boolean firstScan = true;

    private final static int WIFI_SCAN_DURATION = 6000;

    public WifiMeasurementFragment() {
    }

    /** STARTUP CODE **/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        measurementAdapter = new MeasurementArrayAdapter(getActivity());
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        super.onCreate(savedInstanceState);
        try {
            startupCode();
        }
        catch (Exception ex) {

            ex.printStackTrace();

        }
    }


    /**
     * Wifi startup method. Starts the pressure sensor, obtains the user's previous settings,
     * and starts the wifi scanning process.
     */
    private void startupCode() throws InterruptedException {
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor != null) {
            pressureListener = new PressureListener();
            mSensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        myContext = WifiMeasurementFragment.this.getActivity().getApplicationContext();
        wm = (WifiManager)  getActivity().getSystemService(Context.WIFI_SERVICE);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        isInstantWifi = sharedPref.getBoolean("pref_wifi_instant", true);
        Boolean showStatistics = sharedPref.getBoolean("pref_statistics_display", true);

        wifiStuff = new WifiResults(this,showStatistics);
        startWifiMeasurement();
    }

    //**GENERIC INFO**/

    /**
     * Called by the parent activity when it's connected to the Google API; starts an AsyncTask to obtain the location-relevant data.
     */
    public void getGenericInfo(){
        genericResults = new GenericResults(measurementAdapter);
        ConnectivityManager cm = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        Boolean isConnected = AuxiliaryMethods.isActiveNetworkConnected(cm);
        if (pressureListener != null) {
            mSensorManager.registerListener(pressureListener, pressureSensor, 1);
            new WifiGenericTask().execute(mLastLocation, pressureListener, genericResults, isConnected);
        }
        else
            new WifiGenericTask().execute(mLastLocation, null, genericResults, isConnected);
    }

    public void setLocation(Location location) {
        mLastLocation = location;
    }

    /**
     * Wifi implementation of the AsyncTask.
     */
    private class WifiGenericTask extends GenericInfoTask {
        @Override
        protected void onPostExecute(GenericResults genericResults) {
            super.onPostExecute(genericResults);

            String id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == (null)) id = "-1";
            genericResults.setUserID(id);

            printGenericJSON();
        }
    }

    /**
     * Called on AsyncTask completion, finishes out the JSON for the generic info - only saves it to SQL if
     * the wifi scan's already completed.
     */
    public void printGenericJSON(){
        genericResults.returnResults();
        genericJSON = genericResults.jsonString;
        if (!measurementComplete && wifiJSON!=null) {
            AuxiliaryMethods.saveToSQL(genericJSON, wifiJSON);
            measurementComplete = true;
            myContext.unregisterReceiver(wifiReceiver);
            mSensorManager.unregisterListener(pressureListener);
        }
    }


    /** WIFI STUFF **/
    /**
     * Enables wi-fi if it's disabled, then, depending on whether the user only wants one scan or multiple,
     * starts a different receiver.
     */
    private void startWifiMeasurement() throws InterruptedException {
        if (!wm.isWifiEnabled()) {
            if (getActivity()!=null)
                SnackbarManager.show(
                        Snackbar.with(WifiMeasurementFragment.this.getActivity()).text("Wifi disabled, enabling!")
                                .actionLabel("close")
                                .actionColor(Color.WHITE));
            wm.setWifiEnabled(true);
            while(wm.getWifiState()!=WifiManager.WIFI_STATE_ENABLED){ //TODO: callback/timeout
                //wait
            }
        }
        if (getActivity()!=null)
            SnackbarManager.show(
                    Snackbar.with(WifiMeasurementFragment.this.getActivity()).text("Starting, please wait...")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));

        currentInfo = wm.getConnectionInfo();
        currBSSID = currentInfo.getBSSID();

        if (isInstantWifi) {
            wifiReceiver = new WifiReceiver();
            myContext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            wm.startScan();
        }

        else {
            wifiReceiver = new IterativeWifiReceiver();
            myContext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            iterateWifiMeasurement();
        }
    }

    /**
     * Regular wifi BroadcastReceiver implementation. Receives multiple scan results, when it's done processing,
     * calls the finishWifiMeasurement() method.
     */
    class WifiReceiver extends BroadcastReceiver { //see http://www.androidsnippets.com/scan-for-wireless-networks
        StringBuilder sb;
        List<ScanResult> wifiList;

        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();
            wifiList = wm.getScanResults();
            wifiStuff.setTimestamp(AuxiliaryMethods.getTimeStamp());
            for(ScanResult s : wifiList){
                ScanResultObject res = new ScanResultObject();
                res.setSSID(s.SSID);
                res.setBSSID(s.BSSID);
                res.setSignalStrength(Integer.toString(s.level));
                res.setChannel(calcChannel(s.frequency));
                res.setSecurity(getSecurity(s));
                res.setKeyManagement(getKeyManagement(s));
                res.setEncryption(getEncryption(s));
                res.setHasWPS(hasWPS(s.capabilities));
                res.setIsPublicWifi(isPublicWifi(s.SSID));
                res.setServiceSet(parseServiceSet(s.capabilities));
                if (currBSSID!=null && currBSSID.equals(s.BSSID)){
                    res.setLinkSpeed(currentInfo.getLinkSpeed());
                    res.setHasHiddenSSID(currentInfo.getHiddenSSID());
                }
                wifiStuff.getWifiScanResults().add(res);
            }
            finishWifiMeasurement();
        }
    }

    /**
     * Alternate BroadcastReceiver that performs an average over time of the wifi network signals.
     */
    class IterativeWifiReceiver extends BroadcastReceiver {
        StringBuilder sb;
        List<ScanResult> wifiList;

        public void onReceive(Context c, Intent intent) {
            sb = new StringBuilder();
            wifiList = wm.getScanResults();
            wifiStuff.setTimestamp(AuxiliaryMethods.getTimeStamp());
            HashMap<String, Integer> signals = new HashMap<>();
            for (ScanResult s : wifiList) {
                if (firstScan) {
                    ScanResultObject res = new ScanResultObject();
                    res.setSSID(s.SSID);
                    res.setBSSID(s.BSSID);
                    signals.put(s.BSSID, s.level);
                    res.setChannel(calcChannel(s.frequency));
                    res.setSecurity(getSecurity(s));
                    res.setKeyManagement(getKeyManagement(s));
                    res.setEncryption(getEncryption(s));
                    res.setHasWPS(hasWPS(s.capabilities));
                    res.setIsPublicWifi(isPublicWifi(s.SSID));
                    res.setServiceSet(parseServiceSet(s.capabilities));
                    if (currBSSID != null && currBSSID.equals(s.BSSID)) {
                        res.setLinkSpeed(currentInfo.getLinkSpeed());
                        res.setHasHiddenSSID(currentInfo.getHiddenSSID());
                    }
                    wifiStuff.getWifiScanResults().add(res);
                } else {
                    signals.put(s.BSSID, s.level);
                }
            }
            if (!firstScan)
                for (ScanResultObject b : wifiStuff.getWifiScanResults()) {
                    Integer signal = signals.get(b.getBSSID());
                    if (signal!=null)
                        b.signalsList.add(signal);
                }
            firstScan = false;
        }
    }

    /**
     * Performs as many scans at it can for WIFI_SCAN_DURATION milliseconds.
     */
    private void iterateWifiMeasurement() {
        new CountDownTimer(WIFI_SCAN_DURATION,1) {
        public void onTick(long millisUntilFinished){
            wm.startScan();
        }

        public void onFinish() {
            wifiStuff.processSignals();
            if (getActivity()!=null)
                SnackbarManager.show(
                        Snackbar.with(WifiMeasurementFragment.this.getActivity()).text("Measurements completed!")
                                .actionLabel("close")
                                .actionColor(Color.WHITE));
            finishWifiMeasurement();
        }
    }.start();

    }

    /**
     * Finishes out the JSON for the wifi info - only saves it to SQL if
     * the generic info getting is already completed.
     */
    private void finishWifiMeasurement(){
        wifiJSON = wifiStuff.returnResults();
        if (getActivity()!=null)
            SnackbarManager.show(
                    Snackbar.with(WifiMeasurementFragment.this.getActivity()).text("Measurements completed!")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));
        if (!measurementComplete && genericJSON!=null) {
            AuxiliaryMethods.saveToSQL(genericJSON, wifiJSON);
            measurementComplete = true;
            myContext.unregisterReceiver(wifiReceiver);
            mSensorManager.unregisterListener(pressureListener);
        }
    }

    /** ANDROID METHODS **/
    @Override
    public void onPause() {
        super.onPause();
        try {
            myContext.unregisterReceiver(wifiReceiver);
            mSensorManager.unregisterListener(pressureListener);
        }
        catch(IllegalArgumentException e){
            //do nothing, receiver is already unregistered
        }
    }

    @Override
    public void onResume() {
        myContext.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);



        ListView listView = (ListView) rootView.findViewById(R.id.measurementListView);
        listView.setAdapter(measurementAdapter);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_measurement, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                measurementAdapter.clear();
                genericJSON = null;
                wifiJSON = null;
                measurementComplete = false;
                firstScan = true;
                try {
                    getGenericInfo();
                    startupCode();
                } catch (InterruptedException e) {
                    //TODO: handle exception
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    /** AUXILIARY METHODS **/
    private Boolean hasWPS(String capabilities) {
        return (capabilities.contains("WPS"));
    }

    /***
     * Calculates the wi-fi channel based on a given frequency.
     * */
    private int calcChannel(int frequency) { //http://www.radio-electronics.com/info/wireless/wi-fi/80211-channels-number-frequencies-bandwidth.php & http://stackoverflow.com/a/11744654
        if (frequency<=2600) {
            final ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
                    Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
                            2452, 2457, 2462, 2467, 2472, 2484));
            return channelsFrequency.indexOf(Integer.valueOf(frequency));
        }
        else{
            switch(frequency){
                case 5180:
                    return 36;
                case 5200:
                    return 40;
                case 5220:
                    return 44;
                case 5240:
                    return 48;
                case 5260:
                    return 52;
                case 5280:
                    return 56;
                case 5300:
                    return 60;
                case 5320:
                    return 64;
                case 5500:
                    return 100;
                case 5520:
                    return 104;
                case 5540:
                    return 108;
                case 5560:
                    return 112;
                case 5580:
                    return 116;
                case 5600:
                    return 120;
                case 5620:
                    return 124;
                case 5640:
                    return 128;
                case 5660:
                    return 132;
                case 5680:
                    return 136;
                case 5700:
                    return 140;
                case 5745:
                    return 149;
                case 5765:
                    return 153;
                case 5785:
                    return 157;
                case 5805:
                    return 161;
                case 5825:
                    return 165;
                default:
                    return -1;
            }
        }
    }

    private String parseServiceSet(String capabilities) {
        if (capabilities.contains("[ESS]")) return "ESS";
        else if (capabilities.contains("[IBSS]")) return "IBSS";
        else if (capabilities.contains("[BSS]")) return "BSS";
        else return "";
    }

    public static String getSecurity(ScanResult scanResult) { // see android class AccessPointState.java
        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WPA2", "WPA", "WEP" };
        for (int i = 0; i < securityModes.length; i++) {
            if (cap.contains(securityModes[i])) {
                if (securityModes[i].equals("WPA2") & cap.contains("[WPA-"))
                        return "WPA+WPA2";
                else if (securityModes[i].equals("WPA") & cap.contains("[WPA2-"))
                    return "WPA+WPA2";
                else
                    return securityModes[i];
            }
        }

        return "OPEN";
    }

    public static String getKeyManagement(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] keyManagementModes = { "WEP", "PSK", "EAP" };
        for (int i = 0; i < keyManagementModes.length; i++) {
            if (cap.contains(keyManagementModes[i])) {
                return keyManagementModes[i];
            }
        }

        return "OPEN";
    }

    public static String getEncryption(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] encryptionModes = { "CCMP+TKIP", "CCMP", "TKIP"};
        for (int i = 0; i < encryptionModes.length; i++) {
            if (encryptionModes[i].equals("CCMP") & cap.contains("TKIP"))
                return "CCMP+TKIP";
            else if (encryptionModes[i].equals("TKIP") & cap.contains("CCMP"))
                return "CCMP+TKIP";
            else if (cap.contains(encryptionModes[i])) {
                return encryptionModes[i];
            }
        }

        return "OPEN";
    }

    public Boolean isPublicWifi(String ssid){ // see https://wifi.meo.pt/pt/onde/Pages/Parceiros.aspx
        final String[] publicWifiNames = { "eduroam", "FON", "MEO-WIFI", "openwireless.org", "BTOpenzone", "vex", "homerun", "FREE_WIFI" };
        for (String s : publicWifiNames) {
            if (ssid.contains(s)) {
                return true;
            }
        }
        return false;
    }
}

