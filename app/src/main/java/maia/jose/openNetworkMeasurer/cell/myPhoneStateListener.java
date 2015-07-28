package maia.jose.openNetworkMeasurer.cell;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;

import maia.jose.openNetworkMeasurer.AuxiliaryMethods;
import maia.jose.openNetworkMeasurer.MainActivity;

class myPhoneStateListener extends PhoneStateListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private int phoneType;
    private int networkType;

    public Integer finalAsu;
    public Integer finalDbm;

    private CellResults results;
    private LinkedList<Integer> measurements;

    public myPhoneStateListener(CellResults results){
        this.results = results;
        TelephonyManager tm = results.tm;

        measurements = new LinkedList<>();

        phoneType = tm.getPhoneType();
        networkType = tm.getNetworkType();

        getPhoneType();
        getNetworkTypeString();

        results.setTimestamp(AuxiliaryMethods.getTimeStamp());
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {

        super.onSignalStrengthsChanged(signalStrength);

        getMeasurement(signalStrength);
    }

    private void getNetworkTypeString() {
        String networkTypeString;
        switch (networkType){
            case (TelephonyManager.NETWORK_TYPE_1xRTT):
                networkTypeString = "1xRTT";
                break;
            case (TelephonyManager.NETWORK_TYPE_CDMA):
                networkTypeString = "CDMA"; // can be IS95A or IS95B
                break;
            case (TelephonyManager.NETWORK_TYPE_EDGE):
                networkTypeString = "EDGE";
                break;
            case (TelephonyManager.NETWORK_TYPE_EHRPD):
                networkTypeString = "EHRPD";
                break;
            case (TelephonyManager.NETWORK_TYPE_EVDO_0):
                networkTypeString = "EVDO_0";
                break;
            case (TelephonyManager.NETWORK_TYPE_EVDO_A):
                networkTypeString = "EVDO_A";
                break;
            case (TelephonyManager.NETWORK_TYPE_EVDO_B):
                networkTypeString = "EVDO_B";
                break;
            case (TelephonyManager.NETWORK_TYPE_GPRS):
                networkTypeString = "GPRS";
                break;
            case (TelephonyManager.NETWORK_TYPE_HSDPA):
                networkTypeString = "HSDPA";
                break;
            case (TelephonyManager.NETWORK_TYPE_HSPA):
                networkTypeString = "HSPA";
                break;
            case (TelephonyManager.NETWORK_TYPE_HSPAP):
                networkTypeString = "HSPA+";
                break;
            case (TelephonyManager.NETWORK_TYPE_HSUPA):
                networkTypeString = "HSUPA";
                break;
            case (TelephonyManager.NETWORK_TYPE_IDEN):
                networkTypeString = "IDEN";
                break;
            case (TelephonyManager.NETWORK_TYPE_LTE):
                networkTypeString = "LTE";
                break;
            case (TelephonyManager.NETWORK_TYPE_UMTS):
                networkTypeString = "UMTS";
                break;
            default:
                networkTypeString = "UNKNOWN"; //TODO: exception
                break;
        }
        results.setNetworkTypeString(networkTypeString);
        results.setNetworkClass(AuxiliaryMethods.getNetworkClass(networkType));
    }

    private void getPhoneType() {
        String phoneTypeString;
        switch (phoneType) {
            case (TelephonyManager.PHONE_TYPE_GSM):
                phoneTypeString = "GSM";
                break;

            case (TelephonyManager.PHONE_TYPE_CDMA):
                phoneTypeString = "CDMA";
                break;
            default:
                 phoneTypeString = "ERROR";
        }
        results.setPhoneTypeString(phoneTypeString);
    }

    private void getMeasurement(SignalStrength signalStrength){
        if (networkType != TelephonyManager.NETWORK_TYPE_LTE) {
            switch (phoneType) {
                case (TelephonyManager.PHONE_TYPE_GSM): //also works for UMTS/WCDMA
                    measurements.add(signalStrength.getGsmSignalStrength());
                    break;

                case (TelephonyManager.PHONE_TYPE_CDMA):
                    measurements.add(signalStrength.getCdmaDbm());
                    break;
                default:
                    Log.v(LOG_TAG, "invalid measurement"); //TODO: change
            }
        }
        else { //resort to reflection if the network is LTE (see http://blog.ajhodges.com/2013/03/reading-lte-signal-strength-rssi-in.html)
            try {
                Method[] methods = SignalStrength.class.getMethods();
                for (Method mthd : methods) { // Firefox uses RSRP (per https://mozilla-ichnaea.readthedocs.org/en/latest/cell.html)
                    if (mthd.getName().equals("getLteRsrp")){ //see also mthd.getName().equals("getLteRssi") || mthd.getName().equals("getLteSignalStrength")
                        measurements.add((Integer) mthd.invoke(signalStrength));
                        break;
                    }
                }
            } catch (InvocationTargetException e) { //TODO: handle exceptions correctly
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void calcStrength (){
        Integer[] toAvg = new Integer[measurements.size()];
        measurements.toArray(toAvg);

        int tempAvg;
        Double stdev=0.0;
        Integer max=toAvg[0];
        Integer min=toAvg[0];
        if (measurements.contains(99)) tempAvg = 99;
        else {
            Double rawAvg = AuxiliaryMethods.calcAvg(toAvg);
            stdev = AuxiliaryMethods.calcStDev(toAvg,rawAvg);
            tempAvg = (int) Math.round(rawAvg);
            max = Collections.max(measurements);
            min = Collections.min(measurements);
        }

        String signalStrengthString;
        if (networkType != TelephonyManager.NETWORK_TYPE_LTE) {
            switch (phoneType) {
                case (TelephonyManager.PHONE_TYPE_GSM):
                    if (tempAvg != 99)
                        signalStrengthString = tempAvg + " ASU, " + AuxiliaryMethods.gsmAsuToRSSI(tempAvg) + " dBm";
                    else
                        signalStrengthString = tempAvg + " ASU (error)";
                    this.finalAsu = tempAvg;
                    this.finalDbm = AuxiliaryMethods.gsmAsuToRSSI(tempAvg);
                    break;

                case (TelephonyManager.PHONE_TYPE_CDMA):
                    this.finalDbm = tempAvg;
                    this.finalAsu = AuxiliaryMethods.cdmaRSSIToAsu(tempAvg);
                    signalStrengthString = this.finalAsu + " ASU, " + finalDbm + " dBm";
                    break;
                default:
                    signalStrengthString = "something broke!";
                    Log.v(LOG_TAG, "invalid signal strengths"); //TODO: change
            }
        }
        else { //is LTE
            //-45 to -137 dBm
            this.finalDbm = tempAvg;
            this.finalAsu = AuxiliaryMethods.lteRSRPtoAsu(tempAvg);
            signalStrengthString = this.finalAsu + " ASU, " + finalDbm + " dBm";
        }
        results.setSignalStrengthString(signalStrengthString);
        results.setMeasurementList(measurements.toString());
        results.setStdev(stdev);
        results.setMaxSignal(max.toString());
        results.setMinSignal(min.toString());
    }
}