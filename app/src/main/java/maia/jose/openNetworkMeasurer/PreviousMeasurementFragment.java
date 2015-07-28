package maia.jose.openNetworkMeasurer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import maia.jose.openNetworkMeasurer.sqlite.SQLiteJSONBean;

public class PreviousMeasurementFragment extends Fragment {

    private final String LOG_TAG = PreviousMeasurementActivity.class.getSimpleName();
    private PreviousMeasurementActivity previousMeasurementActivity;
    private long currRecord;
    private Long minRecord = 1L;
    private Long maxRecord;

    public PreviousMeasurementFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        previousMeasurementActivity = (PreviousMeasurementActivity) this.getActivity();
        startupCode();
    }

    private void startupCode() {
        currRecord = SQLiteJSONBean.count(SQLiteJSONBean.class,null,null); //most recent
        maxRecord = currRecord;
        SQLiteJSONBean current = SQLiteJSONBean.findById(SQLiteJSONBean.class,currRecord);
        previousMeasurementActivity.setJSONText(current.getJson());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_previous_measurement, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_next:
                switchJSON(1);
                return true;
            case R.id.action_previous:
                switchJSON(-1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void switchJSON(int i) {
        if (currRecord+i>minRecord && currRecord+i < maxRecord){
            currRecord = currRecord+i;
            SQLiteJSONBean current = SQLiteJSONBean.findById(SQLiteJSONBean.class,currRecord);
            previousMeasurementActivity.setJSONText(current.getJson());
        }
    }
}
