package maia.jose.openNetworkMeasurer.cell;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import maia.jose.openNetworkMeasurer.AuxiliaryMethods;
import maia.jose.openNetworkMeasurer.GenericInfoTask;
import maia.jose.openNetworkMeasurer.GenericResults;
import maia.jose.openNetworkMeasurer.PressureListener;
import maia.jose.openNetworkMeasurer.R;
import maia.jose.openNetworkMeasurer.layout.MeasurementArrayAdapter;

public class CellularMeasurementFragment extends Fragment {

    private final String LOG_TAG = CellularMeasurementActivity.class.getSimpleName();

    public MeasurementArrayAdapter measurementAdapter;
    private CellResults cellStuff;
    private TelephonyManager tm;
    private myPhoneStateListener psListener;
    private String genericJSON;
    private String cellJSON;
    private SensorManager mSensorManager = null;
    private PressureListener pressureListener;
    private GenericResults genericResults;
    private Location mLastLocation;
    private boolean measurementComplete = false;
    private Sensor pressureSensor;

    private final static int MEASUREMENT_DURATION = 5000;

    public CellularMeasurementFragment() {
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
     * Cell startup method. Starts the pressure sensor, obtains the user's previous settings,
     * and starts the cell scanning process.
     */
    private void startupCode() {
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor != null) {
            pressureListener = new PressureListener();
            mSensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        tm = (TelephonyManager)  getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        Boolean showStatistics = sharedPref.getBoolean("pref_statistics_display", true);
        cellStuff = new CellResults(tm,this,showStatistics);
        psListener = new myPhoneStateListener(cellStuff);

        addTelephonyStuff();
        startCellMeasurement();
    }

    //**GENERIC INFO**/

    /**
     * Called by the parent activity when it's connected to the Google API; starts an AsyncTask to obtain the location-relevant data.
     */
    public void getGenericInfo(){
        genericResults = new GenericResults(measurementAdapter);
        ConnectivityManager cm = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        Boolean isConnected = AuxiliaryMethods.isActiveNetworkConnected(cm);
        if (pressureSensor != null) {
            new CellularGenericTask().execute(mLastLocation, pressureListener, genericResults, isConnected);
        }
        else
            new CellularGenericTask().execute(mLastLocation, null, genericResults, isConnected);
    }

    public void setLocation(Location location) {
        mLastLocation = location;
    }

    /**
     * Cell implementation of the AsyncTask.
     */
    private class CellularGenericTask extends GenericInfoTask {
        @Override
        protected void onPostExecute(GenericResults genericResults) {
            super.onPostExecute(genericResults);

            String id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == (null)) id = "-1";
            genericResults.setUserID(id);

            printGenericJSON();
            mSensorManager.unregisterListener(pressureListener);
        }
    }

    /**
     * Called on AsyncTask completion, finishes out the JSON for the generic info - only saves it to SQL if
     * the cell scan's already completed.
     */
    public void printGenericJSON(){
        genericResults.returnResults();
        genericJSON = genericResults.jsonString;
        if (!measurementComplete && cellJSON!=null) {
            AuxiliaryMethods.saveToSQL(genericJSON, cellJSON);
            measurementComplete = true;
        }
    }


    /** CELL STUFF **/

    /**
     * Adds to the results the data that we get directly from the TelephonyManager.
     */
    private void addTelephonyStuff() {
        String temp = tm.getNetworkOperatorName(); // unreliable on CDMA, care
        if (temp != null) {
            cellStuff.setOperatorName(temp);
        }
        temp = tm.getNetworkOperator(); // unreliable on CDMA, care
        if (temp != null) {
            cellStuff.setOperatorID(temp);
        }
        temp = tm.getSimOperatorName();
        if (temp != null ) {
            cellStuff.setSimOperatorName(temp); // SIM options - use when CDMA returns null only?
        }
        temp = tm.getSimOperator();
        if (temp != null) {
            cellStuff.setSimOperatorID(temp);
        }
    }

    /**
     * Starts the PhoneStateListener, waits a few seconds, and then does the required math and
     * saves stuff to SQL if the generic info is done.
     */
    private void startCellMeasurement(){
        tm.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        if (getActivity()!=null)
            SnackbarManager.show(
                    Snackbar.with(CellularMeasurementFragment.this.getActivity()).text("Starting, please wait...")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));

        new CountDownTimer(MEASUREMENT_DURATION,MEASUREMENT_DURATION) {
        public void onTick(long millisUntilFinished){
        }

        public void onFinish() {
            tm.listen(psListener, PhoneStateListener.LISTEN_NONE);
            psListener.calcStrength();
            if (getActivity()!=null)
                SnackbarManager.show(
                    Snackbar.with(CellularMeasurementFragment.this.getActivity()).text("Measurements completed.")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));
            addCellInfos();

            cellJSON = cellStuff.returnResults();
            if (!measurementComplete && genericJSON!=null) {
                AuxiliaryMethods.saveToSQL(genericJSON, cellJSON);
                measurementComplete = true;
            }
        }
        }.start();
    }


    /**
     * Adds all of the information we can get on the cells - first off, it
     * tries to get the AllCellInfo, if it is available,
     * and then the neighbouring cell info, which is
     * accessible on all APIs, whether the AllCellInfo returned anything or not.
     *
     * However, if it only accesses the NeighboringCellInfo, it builds an extra
     * cell manually based off the current CellLocation.
     */
    private void addCellInfos(){
        List<NeighboringCellInfo> cell = tm.getNeighboringCellInfo();

        List<CellInfo> cell2 = new LinkedList<>();
        if (Build.VERSION.SDK_INT>=18) {
            try {
                Method[] methods = TelephonyManager.class.getMethods();
                for (Method mthd : methods) {
                    if (mthd.getName().equals("getAllCellInfo")) {
                        cell2 = getAllCellInfo(mthd);
                    }
                }
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (cell2==null || cell2.isEmpty()) {
            CellLocation location = tm.getCellLocation();
            processCellLocation(location);
        }
        else{
            processAllCellInfo(cell2);
        }
        processNeighboringCellInfo(cell);
     }

    /**
     * Processes the NeighboringCellInfo list.
     */
    private void processNeighboringCellInfo(List<NeighboringCellInfo> cell) {
        for (NeighboringCellInfo nci : cell) {
            CellData current = new CellData();
            current.setRadio(AuxiliaryMethods.getCellRadioTypeName(nci.getNetworkType()));
            String mccMnc = getMccMnc();
            current.setMcc(Integer.parseInt(mccMnc.substring(0, 3)));

            if (current.getRadio().equals("gsm")){
                current.setMnc(Integer.parseInt(mccMnc.substring(3)));
                current.setLac(nci.getLac());
                current.setCid(nci.getCid());
                current.setAsu(nci.getRssi());
                current.setSignal(AuxiliaryMethods.gsmAsuToRSSI((nci.getRssi())));
            }
            else if (current.getRadio().equals("umts")){
                current.setMnc(Integer.parseInt(mccMnc.substring(3)));
                current.setPsc(nci.getPsc());
                current.setSignal(nci.getRssi());
                current.setAsu(AuxiliaryMethods.umtsRSCPToAsu(nci.getRssi()));
            }
            else { //CDMA
                current.setSignal(nci.getRssi()); //we don't really get any decent info from CDMA neighboring cells...
                current.setAsu(AuxiliaryMethods.cdmaRSSIToAsu(nci.getRssi()));
            }
            cellStuff.getCellDatas().add(current);
        }
    }

    /**
     * Manually creates an instance of our CellData based off the current CellLocation and the
     * values calculated in the phoneStateListener.
     */
    private void processCellLocation(CellLocation location) {
        CellData current = new CellData();
        current.setRadio(AuxiliaryMethods.getCellRadioTypeName(tm.getNetworkType()));
        current.setSignal(psListener.finalDbm);
        String mccMnc = getMccMnc();
        current.setMcc(Integer.parseInt(mccMnc.substring(0, 3)));
        if (psListener.finalAsu != null)
            current.setAsu(psListener.finalAsu);

        if (location instanceof GsmCellLocation){
            current.setMnc(Integer.parseInt(mccMnc.substring(3)));
            current.setLac(((GsmCellLocation) location).getLac());
            current.setCid(((GsmCellLocation) location).getCid());
            current.setPsc(((GsmCellLocation) location).getPsc());
            cellStuff.getCellDatas().add(current);
        }
        else if (location instanceof CdmaCellLocation){
            current.setMnc(((CdmaCellLocation) location).getSystemId());
            current.setLac(((CdmaCellLocation) location).getNetworkId());
            current.setCid(((CdmaCellLocation) location).getBaseStationId());
            cellStuff.getCellDatas().add(current);
        }
        else{
            throw new IllegalArgumentException("Unexpected CellLocation type: " + location.getClass().getName());
        }
    }

    private String getMccMnc() {
        String mccMnc = cellStuff.getOperatorID();
        if (mccMnc.length() != 6) //TODO. add other conditions?
            mccMnc = cellStuff.getSimOperatorID();
        return mccMnc;
    }

    @TargetApi(18)
    private List<CellInfo> getAllCellInfo(Method mthd) throws InvocationTargetException, IllegalAccessException {
        return (List<CellInfo>) mthd.invoke(tm);
    }

    /**
     * Processes the AllCellInfo data.
     */
    @TargetApi(18)
    private void processAllCellInfo(List<CellInfo> cell2){
        //TODO: verify integrity of values (e.g. when lac/cid are invalid in LTE/UMTS if a valid psc is given)
        LinkedList<CellData> cellDatas = cellStuff.getCellDatas();
        for (int x=0; x<cell2.size(); x++){
            CellData result = new CellData();
            CellInfo currentCell = cell2.get(x);
            if (currentCell instanceof CellInfoGsm){
                CellSignalStrengthGsm strength = ((CellInfoGsm) currentCell).getCellSignalStrength();
                CellIdentityGsm identity = ((CellInfoGsm) currentCell).getCellIdentity();
                result.setSignal(strength.getDbm());
                if (isInvalidGsm(identity)) return; //WCDMA result showing up as GSM, discard
                result.setAsu(strength.getAsuLevel());
                result.setCid(identity.getCid());
                result.setLac(identity.getLac());
                result.setMcc(identity.getMcc());
                result.setMnc(identity.getMnc());
                result.setRadio("gsm");
                //cannot obtain gsm timing advance from API
            }
            else if (currentCell instanceof CellInfoCdma){
                CellSignalStrengthCdma strength = ((CellInfoCdma) currentCell).getCellSignalStrength();
                CellIdentityCdma identity = ((CellInfoCdma) currentCell).getCellIdentity();
                result.setSignal(strength.getDbm());
                result.setAsu(strength.getAsuLevel());
                result.setCid(identity.getBasestationId());
                result.setLac(identity.getNetworkId());
                result.setMcc(Integer.parseInt(getMccMnc().substring(0, 3)));
                result.setMnc(identity.getSystemId());
                result.setRadio("cdma");
            }
            else if (currentCell instanceof CellInfoLte){
                CellSignalStrengthLte strength = ((CellInfoLte) currentCell).getCellSignalStrength();
                CellIdentityLte identity = ((CellInfoLte) currentCell).getCellIdentity();
                result.setSignal(strength.getDbm());
                result.setAsu(strength.getAsuLevel());
                if ((strength.getTimingAdvance() == Integer.MAX_VALUE)) result.setTa(-1);
                else
                    result.setTa(strength.getTimingAdvance());
                result.setPsc(identity.getPci());
                if (0<=identity.getPci() & identity.getPci()<=503 & (identity.getTac()==-1) || (identity.getTac() == Integer.MAX_VALUE) ){ //make consistent with neighboring
                    result.setLac(-1);
                    result.setCid(-1);
                    String mccMnc = getMccMnc();
                    result.setMcc(Integer.parseInt(mccMnc.substring(0, 3)));
                    result.setMnc(Integer.parseInt(mccMnc.substring(3)));
                }
                else {
                    result.setCid(identity.getCi());
                    result.setLac(identity.getTac());
                    result.setMcc(identity.getMcc());
                    result.setMnc(identity.getMnc());
                }
                result.setRadio("lte");
            }

            else if (currentCell instanceof CellInfoWcdma){
                CellSignalStrengthWcdma strength = ((CellInfoWcdma) currentCell).getCellSignalStrength();
                CellIdentityWcdma identity = ((CellInfoWcdma) currentCell).getCellIdentity();
                result.setSignal(strength.getDbm());
                result.setAsu(strength.getAsuLevel());
                result.setPsc(identity.getPsc());
                if (0<=identity.getPsc() & identity.getPsc()<=511 & (identity.getCid()==-1) || (identity.getCid() == Integer.MAX_VALUE) ){ //make consistent with neighboring
                    result.setLac(-1);
                    result.setCid(-1);
                    String mccMnc = getMccMnc();
                    result.setMcc(Integer.parseInt(mccMnc.substring(0, 3)));
                    result.setMnc(Integer.parseInt(mccMnc.substring(3)));
                }
                else {
                    result.setCid(identity.getCid());
                    result.setLac(identity.getLac());
                    result.setMcc(identity.getMcc());
                    result.setMnc(identity.getMnc());
                }
                result.setRadio("umts");
            }

            cellDatas.add(result);
        }
    }

    /**
     * If a GSM cell measurement shows up with invalid values, it should be discarded.
     */
    @TargetApi(18)
    private boolean isInvalidGsm(CellIdentityGsm identity) {
        return identity.getLac() == Integer.MAX_VALUE ||
                identity.getLac() == 0 ||
                identity.getCid() == 0 ||
                identity.getCid() == Integer.MAX_VALUE ||
                identity.getCid() == 65535 ||
                identity.getCid() == -1 ||
                identity.getMcc() == 0 ||
                identity.getMnc() == 0;
    }

    /** ANDROID METHODS **/
    @Override
    public void onPause() {
        super.onPause();
        tm.listen(psListener, PhoneStateListener.LISTEN_NONE);
        mSensorManager.unregisterListener(pressureListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        tm.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        mSensorManager.registerListener(pressureListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
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
        switch (item.getItemId()){
            case R.id.action_refresh:
                measurementAdapter.clear();
                genericJSON = null;
                cellJSON = null;
                measurementComplete = false;
                getGenericInfo();
                startupCode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
