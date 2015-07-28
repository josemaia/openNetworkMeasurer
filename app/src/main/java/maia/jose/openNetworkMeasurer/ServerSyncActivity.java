package maia.jose.openNetworkMeasurer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import maia.jose.openNetworkMeasurer.sqlite.SQLiteJSONBean;
import maia.jose.openNetworkMeasurer.values.myPrivateValues;

public class ServerSyncActivity extends Activity implements View.OnClickListener {
    private Button btnPost;
    private final String LOG_TAG = ServerSyncActivity.class.getSimpleName();
    private TextView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serversync);
        view = (TextView) findViewById(R.id.post_response_textview);
        btnPost = (Button) findViewById(R.id.btnPost);
        btnPost.setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return false;
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

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnPost:
                ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
                Boolean isConnected = AuxiliaryMethods.isActiveNetworkConnected(cm);
                if (isConnected)
                    try {
                        new HttpAsyncTask().execute(myPrivateValues.measurementServerLocation+URLEncoder.encode("jsonservlet","UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                else
                    view.setText("No currently active network.");
                break;
            default:
                break;
        }
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(String... urls) {
            ArrayList<SQLiteJSONBean> beans = (ArrayList<SQLiteJSONBean>) SQLiteJSONBean.listAll(SQLiteJSONBean.class);
            ArrayList<String> strings = new ArrayList<>();
            for (SQLiteJSONBean current : beans) {
                if (!current.getHasSynchronized()) {
                    String response = SendPOST(urls[0], current.getJson());
                    if (!response.equals("")) { // server responded correctly; sets synchronized
                        current.setHasSynchronized(true);
                        current.save();
                        strings.add("Measurement #"+(1+beans.indexOf(current))+" successfully synchronized");
                    }
                    else {
                        strings.add("Measurement #"+(1+beans.indexOf(current))+" synchronization failed");
                    }
                }
                else{
                    strings.add("Skipping measurement #"+(1+beans.indexOf(current))+"(already synchronized)");
                }
            }
            return strings;
        }

        private String SendPOST(String url, String json) {
            InputStream inputStream;
            String result = "";
            int TIMEOUT_VALUE = 1000;
            try {
                URL postUrl = new URL(url);
                HttpURLConnection con = (HttpURLConnection) postUrl.openConnection();
                byte[] jsonBytes = json.getBytes("UTF-8");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", String.valueOf(jsonBytes.length));
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Host", url);
                con.setRequestMethod("POST");
                con.setConnectTimeout(TIMEOUT_VALUE);
                con.setReadTimeout(TIMEOUT_VALUE);
                con.connect();

                DataOutputStream printout = new DataOutputStream(con.getOutputStream());
                printout.write(jsonBytes);
                printout.flush();
                printout.close();

                if (con.getResponseCode() > 300)
                    Log.e(LOG_TAG, AuxiliaryMethods.convertInputStreamToString(con.getErrorStream()));
                else {
                inputStream = con.getInputStream();
                if (inputStream != null) {
                    result = AuxiliaryMethods.convertInputStreamToString(inputStream);
                } else
                    result = "";
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG,e.toString());
            }
            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            StringBuilder b = new StringBuilder();
            for (String s : strings){
                b.append(s).append("\n");
            }
            view.setText(b.toString());
            SnackbarManager.show(
                    Snackbar.with(ServerSyncActivity.this).text("Sync completed.")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));
        }
    }
}
