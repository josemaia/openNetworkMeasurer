package maia.jose.openNetworkMeasurer;

import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import maia.jose.openNetworkMeasurer.values.myPrivateValues;

public class GenericInfoTask
        extends AsyncTask<Object, Void, GenericResults>{
    private final String LOG_TAG = GenericInfoTask.class.getSimpleName();

    GenericResults result;
    Location location;
    String longitude = "";
    String latitude = "";
    float pressure = 0.0f;
    PressureListener listener;
    Boolean isConnected;

    @Override
    protected GenericResults doInBackground(Object... params) {
        this.location= (Location) params[0];
        this.listener= (PressureListener) params[1];
        this.result= (GenericResults) params[2];
        this.isConnected= (Boolean) params[3];
        addBuildStuff();
        addLocationStuff();
        if (listener!=null){
            Log.v(LOG_TAG, "There is a listener. Pressure:"+listener.pressure_value);
            this.pressure = listener.pressure_value;
            result.setPhonePressure(String.valueOf(pressure));
            if (!longitude.equals("") && isConnected)
                getWebPressure(longitude,latitude);
        }
        return result;
    }

    private void getWebPressure(String longitude, String latitude) {
        HttpURLConnection conn;
        try {
            URL weatherUrl = new URL("http://api.wunderground.com/api/" + myPrivateValues.weatherAPIKey + "/conditions/q/" + latitude + "," + longitude + ".json");
            conn = (HttpURLConnection) weatherUrl.openConnection();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(in);
            while (parser.nextToken() != null) {
                parser.nextToken();
                String name = parser.getCurrentName();
                if (name!=null){
                    if (name.equals("pressure_mb")) {
                        parser.nextToken();
                        String value = parser.getText();
                        result.setSeaLevelPressure(value);
                        if (this.pressure!=0.0f){
                            result.setAltitude(SensorManager.getAltitude(Float.valueOf(value),pressure));
                        }
                        break;
                    }
                }
            }
            conn.disconnect();
        }
        catch(IOException e){
            Log.e(LOG_TAG,e.toString());
        }
    }

    @Override
    protected void onPostExecute(GenericResults genericResults) {
        super.onPostExecute(genericResults);
        //implemented in Wifi/Cell fragment
    }

    private void addBuildStuff() {
        if (Build.MANUFACTURER!=null) {
            result.setManufacturer(Build.MANUFACTURER);
        }
        if (Build.MODEL!=null) {
            result.setModel(Build.MODEL);
        }
        if (Build.HARDWARE!=null) {
            result.setHardware(Build.HARDWARE);
        }
        result.setSdkVersion(Build.VERSION.SDK_INT);
        result.setOsVersion(Build.VERSION.RELEASE);
    }

    private void addLocationStuff() {
        if (location!=null) {
            longitude = String.valueOf(location.getLongitude());
            result.setLongitude(longitude);

            latitude = String.valueOf(location.getLatitude());
            result.setLatitude(latitude);

            if (location.hasAccuracy())
                result.setAccuracy(location.getAccuracy());
        }
    }

}