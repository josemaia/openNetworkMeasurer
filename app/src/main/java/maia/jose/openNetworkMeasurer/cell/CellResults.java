package maia.jose.openNetworkMeasurer.cell;

import android.telephony.TelephonyManager;
import android.util.Log;

import com.fasterxml.jackson.jr.ob.JSON;

import java.util.LinkedList;

import maia.jose.openNetworkMeasurer.layout.MeasurementArrayAdapter;
import maia.jose.openNetworkMeasurer.layout.MeasurementEntry;

public class CellResults {
    private final String LOG_TAG = CellResults.class.getSimpleName();

    private CellularMeasurementFragment cellularMeasurementFragment;
    private String timestamp;
    private String networkTypeString;
    private String networkClass;
    private String phoneTypeString;
    private String operatorName;
    private String operatorID;
    private String SimOperatorName;
    private String SimOperatorID;
    private LinkedList<CellData> cellDatas;
    private String measurementList;
    //statistics
    private Double stdev;
    private String maxSignal;
    private String minSignal;
    private String signalStrengthString;

    Boolean showStatistics;
    public TelephonyManager tm;

    CellResults(TelephonyManager tm, CellularMeasurementFragment cellularMeasurementFragment, Boolean showStatistics) {
        this.tm = tm;
        this.cellularMeasurementFragment = cellularMeasurementFragment;
        this.showStatistics = showStatistics;
        cellDatas = new LinkedList<>();
    }

    //getters & setters
    public String getTimestamp() {
        return timestamp;
    }

    public Double getStdev() {
        return stdev;
    }

    public void setStdev(Double stdev) {
        this.stdev = stdev;
    }

    public String getMeasurementList() {
        return measurementList;
    }

    public void setMeasurementList(String measurementList) {
        this.measurementList = measurementList;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    public String getNetworkClass() {
        return networkClass;
    }

    public void setNetworkClass(String networkClass) {
        this.networkClass = networkClass;
    }

    public String getNetworkTypeString() {
        return networkTypeString;
    }

    public void setNetworkTypeString(String networkTypeString) {
        this.networkTypeString = networkTypeString;
    }

    public String getPhoneTypeString() {
        return phoneTypeString;
    }

    public void setPhoneTypeString(String phoneTypeString) {
        this.phoneTypeString = phoneTypeString;
    }

    public String getSignalStrengthString() {
        return signalStrengthString;
    }

    public void setSignalStrengthString(String signalStrengthString) {
        this.signalStrengthString = signalStrengthString;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorID() {
        return operatorID;
    }

    public void setOperatorID(String operatorID) {
        this.operatorID = operatorID;
    }

    public String getSimOperatorName() {
        return SimOperatorName;
    }

    public void setSimOperatorName(String simOperatorName) {
        SimOperatorName = simOperatorName;
    }

    public String getSimOperatorID() {
        return SimOperatorID;
    }

    public void setSimOperatorID(String simOperatorID) {
        SimOperatorID = simOperatorID;
    }

    public LinkedList<CellData> getCellDatas() {
        return cellDatas;
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

    public void setCellDatas(LinkedList<CellData> cellDatas) {
        this.cellDatas = cellDatas;
    }

    String returnResults() {
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Operator ID", getOperatorID()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Operator Name", getOperatorName()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Timestamp", getTimestamp()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Network Type", getNetworkTypeString()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Network Generation", getNetworkClass()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Phone Type", getPhoneTypeString()));
        cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Average Signal Strength (dBm)", getSignalStrengthString()));

        if (showStatistics) {
            cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Collected Measurements", getMeasurementList()));
            cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Standard Deviation", getStdev().toString()));
            cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Maximum Value", getMaxSignal()));
            cellularMeasurementFragment.measurementAdapter.add(new MeasurementEntry("Minimum Value", getMinSignal()));
        }

        for (CellData cd : cellDatas){
            cd.returnResults(cellularMeasurementFragment.measurementAdapter);
        }

        try {
            String jsonString = JSON.std.
                    with(JSON.Feature.PRETTY_PRINT_OUTPUT).
                    asString(this);
            return jsonString;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.toString());
        }
        return ""; //TODO: change failsafe
    }

}

class CellData { // mozilla ichnaea-style classification: https://mozilla-ichnaea.readthedocs.org/en/latest/cell.html
    private String radio; // gsm, umts, lte, cdma
    private Integer mcc; // mobile country code
    private Integer mnc; // mobile network code; system identifier in CDMA
    private Integer lac = -1; // local area code in GSM/UMTS; tracking area code in LTE; network ID in CDMA;
    private Integer cid = -1; // Cell ID; base station ID in CDMA;
    private Integer signal; // Cell signal strength (RSSI in GSM and CDMA / RSCP in UMTS / RSRP in LTE)
    private Integer asu; // arbitrary signal strength
    private Integer ta; //only valid for gsm/lte
    private Integer psc; //only on UMTS and LTE (pci)

    public void setPsc(Integer psc) {
        this.psc = psc;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public Integer getMcc() {
        return mcc;
    }

    public void setMcc(Integer mcc) {
        this.mcc = mcc;
    }

    public Integer getMnc() {
        return mnc;
    }

    public void setMnc(Integer mnc) {
        this.mnc = mnc;
    }

    public Integer getLac() {
        return lac;
    }

    public void setLac(Integer lac) {
        this.lac = lac;
    }

    public Integer getCid() {
        return cid;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public Integer getSignal() {
        return signal;
    }

    public void setSignal(Integer signal) {
        this.signal = signal;
    }

    public Integer getAsu() {
        return asu;
    }

    public void setAsu(Integer asu) {
        this.asu = asu;
    }

    public Integer getTa() {
        return ta;
    }

    public void setTa(Integer ta) {
        this.ta = ta;
    }

    public Integer getPsc() {
        return psc;
    }

    public void returnResults(MeasurementArrayAdapter measurementAdapter) {
        measurementAdapter.add(new MeasurementEntry("Cell Radio Type",radio));
        measurementAdapter.add(new MeasurementEntry("Mobile Country Code",mcc.toString()));

        switch(radio){
            case "gsm":
                measurementAdapter.add(new MeasurementEntry("Mobile Network Code",mnc.toString()));
                measurementAdapter.add(new MeasurementEntry("Location Area Code",lac.toString()));
                measurementAdapter.add(new MeasurementEntry("Cell ID",cid.toString()));
                if (signal != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSSI, dBm)",signal.toString()));
                if (asu != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSSI, ASU)",asu.toString()));
                if (ta != null) measurementAdapter.add(new MeasurementEntry("Timing Advance",ta.toString()));
                break;

            case "umts":
                measurementAdapter.add(new MeasurementEntry("Mobile Network Code",mnc.toString()));
                measurementAdapter.add(new MeasurementEntry("Location Area Code",lac.toString()));
                measurementAdapter.add(new MeasurementEntry("Cell ID",cid.toString()));
                if (psc != null) measurementAdapter.add(new MeasurementEntry("Primary Scrambling Code",psc.toString()));
                if (signal != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSCP, dBm)",signal.toString()));
                if (asu != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSCP, ASU)",asu.toString()));
                break;

            case "lte":
                measurementAdapter.add(new MeasurementEntry("Mobile Network Code",mnc.toString()));
                measurementAdapter.add(new MeasurementEntry("Tracking Area Code",lac.toString()));
                measurementAdapter.add(new MeasurementEntry("Cell Identity",cid.toString()));
                if (psc != null) measurementAdapter.add(new MeasurementEntry("Physical Cell Id",psc.toString()));
                if (signal!= null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSRP, dBm)",signal.toString()));
                if (asu != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSRP, ASU)",asu.toString()));
                if (ta != null) measurementAdapter.add(new MeasurementEntry("Timing Advance",ta.toString()));
                break;

            case "cdma":
                measurementAdapter.add(new MeasurementEntry("System Identifier",mnc.toString()));
                measurementAdapter.add(new MeasurementEntry("Network Identifier",lac.toString()));
                measurementAdapter.add(new MeasurementEntry("Base Station Identifier",cid.toString()));
                if (signal != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSSI, dBm)",signal.toString()));
                if (asu != null) measurementAdapter.add(new MeasurementEntry("Cell Signal Strength (RSSI, ASU)",asu.toString()));
                break;
        }
    }
}

