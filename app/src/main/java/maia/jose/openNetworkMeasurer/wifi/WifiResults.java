package maia.jose.openNetworkMeasurer.wifi;

import android.util.Log;

import com.fasterxml.jackson.jr.ob.JSON;

import java.util.Collections;
import java.util.LinkedList;

import maia.jose.openNetworkMeasurer.AuxiliaryMethods;
import maia.jose.openNetworkMeasurer.layout.MeasurementEntry;

public class WifiResults {
    private final String LOG_TAG = WifiResults.class.getSimpleName();

    private WifiMeasurementFragment wifiMeasurementFragment;
    private String timestamp;
    private LinkedList<ScanResultObject> wifiScanResults;

    private Boolean showStatistics;

    WifiResults(WifiMeasurementFragment wifiMeasurementFragment, Boolean showStatistics){
        this.wifiMeasurementFragment = wifiMeasurementFragment;
        this.showStatistics = showStatistics;
        wifiScanResults = new LinkedList<>();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public LinkedList<ScanResultObject> getWifiScanResults() {
        return wifiScanResults;
    }


    String returnResults(){
        wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Timestamp", timestamp));
        for (ScanResultObject s : getWifiScanResults()) {
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Network SSID", s.getSSID()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Access Point BSSID", s.getBSSID()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Signal Strength (dBm)", s.getSignalStrength()));
            if (s.signalsList != null && !s.signalsList.isEmpty() && showStatistics) {
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Measurements", s.signalsList.toString()));
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Standard Deviation", s.stDev.toString()));
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Maximum Signal", s.maxSignal));
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Minimum Signal", s.minSignal));

            }
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Wifi Channel", s.getChannel().toString()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Security", s.getSecurity()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Key Management", s.getKeyManagement()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Encryption", s.getEncryption()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("WPS?", s.getHasWPS().toString()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Public Wifi?", s.getIsPublicWifi().toString()));
            wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Service Set Type", s.getServiceSet()));
            if (s.hasHiddenSSID != null && s.getLinkSpeed() != null) {
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Hidden SSID?", s.getHasHiddenSSID().toString()));
                wifiMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Link Speed", s.getLinkSpeed().toString() + " Mbps"));
            }
        }
        //JSON
        try {
            String jsonString= JSON.std.
                    with(JSON.Feature.PRETTY_PRINT_OUTPUT).
                    asString(this);
            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.toString());
        }
        return ""; //TODO: change failsafe
    }

    public void processSignals() {
        for (ScanResultObject s : getWifiScanResults()){
            s.processSignals();
        }
    }
}

    class ScanResultObject{
        private final String LOG_TAG = ScanResultObject.class.getSimpleName();

        public String SSID;
        public String BSSID;
        //statistics
        public String signalStrength;
        public String maxSignal;
        public String minSignal;
        public Double stDev;

        public String security;
        public String keyManagement;
        public String encryption;
        public Boolean hasWPS;
        public Boolean isPublicWifi;
        public String serviceSet;
        public Integer channel;
        public Boolean hasHiddenSSID;
        public Integer linkSpeed;
        public LinkedList<Integer> signalsList;

        public ScanResultObject(){
            signalsList = new LinkedList<>();
        }
        public String getSSID() {
            return SSID;
        }

        public void setSSID(String SSID) {
            this.SSID = SSID;
        }

        public String getBSSID() {
            return BSSID;
        }

        public void setBSSID(String BSSID) {
            this.BSSID = BSSID;
        }

        public Integer getChannel() {
            return channel;
        }

        public void setChannel(Integer channel) {
            this.channel = channel;
        }

        public Boolean getHasHiddenSSID() {
            return hasHiddenSSID;
        }

        public void setHasHiddenSSID(Boolean hasHiddenSSID) {
            this.hasHiddenSSID = hasHiddenSSID;
        }

        public Integer getLinkSpeed() {
            return linkSpeed;
        }

        public void setLinkSpeed(Integer linkSpeed) {
            this.linkSpeed = linkSpeed;
        }


        public String getSignalStrength() {
            return signalStrength;
        }

        public void setSignalStrength(String signalStrength) {
            this.signalStrength = signalStrength;
        }

        public String getSecurity() {
            return security;
        }

        public void setSecurity(String security) {
            this.security = security;
        }

        public String getKeyManagement() {
            return keyManagement;
        }

        public void setKeyManagement(String keyManagement) {
            this.keyManagement = keyManagement;
        }

        public String getEncryption() {
            return encryption;
        }

        public void setEncryption(String encryption) {
            this.encryption = encryption;
        }

        public Boolean getHasWPS() {
            return hasWPS;
        }

        public void setHasWPS(Boolean hasWPS) {
            this.hasWPS = hasWPS;
        }

        public Boolean getIsPublicWifi() {
            return isPublicWifi;
        }

        public void setIsPublicWifi(Boolean isPublicWifi) {
            this.isPublicWifi = isPublicWifi;
        }

        public String getServiceSet() {
            return serviceSet;
        }

        public void setServiceSet(String serviceSet) {
            this.serviceSet = serviceSet;
        }

        public String getMaxSignal() {
            return maxSignal;
        }

        public void setMaxSignal(String maxSignal) {
            this.maxSignal = maxSignal;
        }

        public String getMinSignal() {
            return minSignal;
        }

        public void setMinSignal(String minSignal) {
            this.minSignal = minSignal;
        }

        public Double getStDev() {
            return stDev;
        }

        public void setStDev(Double stDev) {
            this.stDev = stDev;
        }

        public void processSignals(){
            Integer[] tempSignals = new Integer[signalsList.size()];
            signalsList.toArray(tempSignals);
            Double tempAvg = AuxiliaryMethods.calcAvg(tempSignals);
            this.stDev = AuxiliaryMethods.calcStDev(tempSignals,tempAvg);
            this.signalStrength = Long.toString(Math.round(tempAvg));
            this.maxSignal = Collections.max(signalsList).toString();
            this.minSignal = Collections.min(signalsList).toString();

        }
    }