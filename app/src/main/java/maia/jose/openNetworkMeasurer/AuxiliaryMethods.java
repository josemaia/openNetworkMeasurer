package maia.jose.openNetworkMeasurer;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import maia.jose.openNetworkMeasurer.sqlite.SQLiteJSONBean;

public class AuxiliaryMethods {
    private final static String LOG_TAG = AuxiliaryMethods.class.getSimpleName();

    public static String getTimeStamp(){
        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        return stamp.toString();
    }

    public static boolean hasPreviousMeasurements() {
        return (0!=SQLiteJSONBean.count(SQLiteJSONBean.class,null,null));
    }

    public static void saveToSQL(String genericJSON, String otherJSON) {
        StringBuilder builder = new StringBuilder();
        builder.append(genericJSON);
        int toDelete = builder.length();
        builder.append(otherJSON);
        builder.replace(toDelete - 2, toDelete + 1, ",");
        //Log.v(LOG_TAG,builder.toString());
        SQLiteJSONBean bean = new SQLiteJSONBean(builder.toString());
        bean.save();
    }

    public static String getNetworkClass(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    public static String getCellRadioTypeName(int networkType) { // via MozStumblr code
        switch (networkType) {
// If the network is either GSM or any high-data-rate variant of it, the radio
// field should be specified as `gsm`. This includes `GSM`, `EDGE` and `GPRS`.
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "gsm";
// If the network is either UMTS or any high-data-rate variant of it, the radio
// field should be specified as `umts`. This includes `UMTS`, `HSPA`, `HSDPA`,
// `HSPA+` and `HSUPA`.
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "umts";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "lte";
// If the network is either CDMA or one of the EVDO variants, the radio
// field should be specified as `cdma`. This includes `1xRTT`, `CDMA`, `eHRPD`,
// `EVDO_0`, `EVDO_A`, `EVDO_B`, `IS95A` and `IS95B`.
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "cdma";
            default:
                Log.e(LOG_TAG, "", new IllegalArgumentException("Unexpected network type: " + networkType));
                return "";
        }
    }

    public static int gsmAsuToRSSI(int signalStrength) {return 2*signalStrength-113;}
    public static int umtsRSCPToAsu(int signalStrength) {return (signalStrength+113)/2;}
    public static Integer lteRSRPtoAsu(int signalStrength) {return signalStrength+140;}

    public static int cdmaRSSIToAsu(int signalStrength){
        if (signalStrength<=-100)
            return 1;
        if (signalStrength<=-95)
            return 2;
        if (signalStrength<=-90)
            return 4;
        if (signalStrength<=-82)
            return 8;
        if (signalStrength<=-75)
            return 16;
        return -1; //should never happen
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public static Boolean isActiveNetworkConnected(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        return !(info==null) && info.isConnected();
    }

    public static Double calcAvg(Integer[] toAvg) {
        Double avg = 0.0;
        if (toAvg.length>0){
            for (Integer i:toAvg){
                avg+=i;
            }
            return (avg / toAvg.length);
        }
        return avg;
    }

    public static Double calcStDev(Integer[] toAvg, Double avg){
        Double stdev = 0.0;
        if (toAvg.length<2) return stdev;
        for (int i : toAvg){
            stdev+=(avg - i) * (avg - i);
        }
        return Math.sqrt(stdev/(toAvg.length-1));
    }

}
