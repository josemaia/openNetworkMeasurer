package maia.jose.openNetworkMeasurer;

import android.util.Log;

import com.fasterxml.jackson.jr.ob.JSON;

import maia.jose.openNetworkMeasurer.layout.MeasurementArrayAdapter;
import maia.jose.openNetworkMeasurer.layout.MeasurementEntry;

public class GenericResults {
    private final String LOG_TAG = GenericResults.class.getSimpleName();

    private String userID;
    private String manufacturer;
    private String model;
    private String hardware;
    private Integer SdkVersion;
    private String OsVersion;
    private String Latitude;
    private String Longitude;
    private Float Accuracy;
    private MeasurementArrayAdapter measurementAdapter;
    private String SeaLevelPressure;
    private String PhonePressure;
    private float altitude = 0.0f;

    public String jsonString;

    public GenericResults(MeasurementArrayAdapter measurementAdapter){
        this.measurementAdapter = measurementAdapter;
    }


    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public Integer getSdkVersion() {
        return SdkVersion;
    }

    public void setSdkVersion(Integer sdkVersion) {
        this.SdkVersion = sdkVersion;
    }

    public String getOsVersion() {
        return OsVersion;
    }

    public void setOsVersion(String osVersion) {
        OsVersion = osVersion;
    }

    public String getLatitude() {
        return Latitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return Longitude;
    }

    public void setLongitude(String longitude) {
        Longitude = longitude;
    }

    public String getSeaLevelPressure() {
        return SeaLevelPressure;
    }

    public void setSeaLevelPressure(String seaLevelPressure) {
        SeaLevelPressure = seaLevelPressure;
    }

    public String getPhonePressure() {
        return PhonePressure;
    }

    public void setPhonePressure(String phonePressure) {
        PhonePressure = phonePressure;
    }


    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public void returnResults(){
        try {
            jsonString= JSON.std.
                    with(JSON.Feature.PRETTY_PRINT_OUTPUT).
                    asString(this);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.toString());
        }

        measurementAdapter.add(new MeasurementEntry("User ID", userID));
        measurementAdapter.add(new MeasurementEntry("Manufacturer",getManufacturer()));
        measurementAdapter.add(new MeasurementEntry("Model",getModel()));
        measurementAdapter.add(new MeasurementEntry("Hardware",getHardware()));
        measurementAdapter.add(new MeasurementEntry("SDK Version", getSdkVersion().toString()));
        measurementAdapter.add(new MeasurementEntry("Android OS Version",getOsVersion()));
        measurementAdapter.add(new MeasurementEntry("Latitude",getLatitude()));
        measurementAdapter.add(new MeasurementEntry("Longitude",getLongitude()));
        if (Accuracy!=null) measurementAdapter.add(new MeasurementEntry("Location Accuracy (m)",String.valueOf(getAccuracy())));
        if (getSeaLevelPressure()!=null) measurementAdapter.add(new MeasurementEntry("Sea Level Pressure (millibar)",getSeaLevelPressure()));
        if (getPhonePressure()!=null) measurementAdapter.add(new MeasurementEntry("Phone Level Pressure (millibar)",getPhonePressure()));
        if (getAltitude()!=0.0) measurementAdapter.add(new MeasurementEntry("Altitude",String.valueOf(getAltitude())));
    }

    public void setAccuracy(float accuracy) {
        this.Accuracy = accuracy;
    }

    public float getAccuracy() {
        return Accuracy;
    }
}
