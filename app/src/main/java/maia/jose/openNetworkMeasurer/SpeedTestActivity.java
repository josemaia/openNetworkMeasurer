package maia.jose.openNetworkMeasurer;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;
import maia.jose.openNetworkMeasurer.values.myPrivateValues;

public class SpeedTestActivity extends AppCompatActivity {
    private final String LOG_TAG = SpeedTestActivity.class.getSimpleName();
    private TextView view;

    private long enqueue;
    private File torrentDestination;
    private BroadcastReceiver receiver;

    private AsyncTask runningTask;
    private Handler handler = new Handler();
    private TaskCanceler taskCanceler;
    private static final int TASK_CANCEL_THRESHOLD = 60000; //1 minute
    private static final long MEGABYTE_IN_BYTES = 1000000;
    private static final long BYTE_LIMIT_THRESHOLD = 20*MEGABYTE_IN_BYTES  ; //20 MB
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speedtest);
        view = (TextView) findViewById(R.id.speedtest_textview);
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        StringBuilder textToSet = new StringBuilder();

        torrentDestination = getExternalCacheDir();
        cleanCache(torrentDestination);

        Button btn_reg_speedtest = (Button) findViewById(R.id.reg_speedtest_btn);
        btn_reg_speedtest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeedtest();
            }
        });

        Button btn_torrent_speedtest = (Button) findViewById(R.id.torrent_speedtest_btn);
        btn_torrent_speedtest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 11) {
                    startTorrentSpeedtest();
                }
                else{
                    SnackbarManager.show(
                            Snackbar.with(SpeedTestActivity.this).text("Not compatible with Android version!")
                                    .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                                    .actionLabel("close")
                                    .actionColor(Color.WHITE));
                }
            }
        });

        if (AuxiliaryMethods.isActiveNetworkConnected(cm)) {
            textToSet.append("Currently active network is: ");
            switch (info.getType()) {
                case ConnectivityManager.TYPE_MOBILE:
                    textToSet.append("Mobile data");
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    textToSet.append("Wifi");
                    break;
                case ConnectivityManager.TYPE_ETHERNET:
                    textToSet.append("Ethernet");
                    break;
                default:
                    textToSet.append("Other");
                    break;
            }
        } else
            textToSet.append("No currently active network");

        if (Build.VERSION.SDK_INT >= 16) {
            if (cm.isActiveNetworkMetered())
                textToSet.append('\n').append("Connection metered. Heavy data usage not recommended " +
                        "due to monetary costs, data limitations or battery/performance issues.");
        } else {
            //implement alternative way to warn?
            if (info.getType() == (ConnectivityManager.TYPE_MOBILE)) {
                textToSet.append('\n').append("Mobile connection. Heavy data usage may be limited " +
                        "due to monetary costs, data limitations or battery/performance issues.");
            }

        }
        view.setText(textToSet.toString());

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setIndeterminateDrawable(new SmoothProgressDrawable.Builder(this)
                .interpolator(new AccelerateInterpolator())
                .colors(getResources().getIntArray(R.array.programColors))
                .build());
        mProgressBar.incrementProgressBy(1);
    }


    private void cleanCache(File dir) {
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()){
                cleanCache(file);
            }
            else {
                boolean wasDeleted = file.delete();
                if (wasDeleted)
                    Log.v(LOG_TAG, "Deleted file " + file.getAbsolutePath());
                else
                    Log.e(LOG_TAG, "Failed to delete file " + file.getAbsolutePath());
            }
        }
    }

    private void startTorrentSpeedtest() {
        final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        receiver = new BroadcastReceiver() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = dm.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c
                                .getInt(columnIndex)) {

                            String uriString = dm.getUriForDownloadedFile(enqueue).getPath();
                            c.close();
                            startTorrent(uriString);
                        }
                    }
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(myPrivateValues.measurementServerLocation + "static/test.torrent"));
        File destination = new File(getExternalCacheDir(), "mytorrent.torrent");
        request.setDestinationUri(Uri.fromFile(destination));
        enqueue = dm.enqueue(request);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (runningTask!=null)
            runningTask.cancel(true);
        if (receiver!=null)
            unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void startTorrent(String uri) {
        view.setText("Performing torrent speedtest...");
        mProgressBar.setVisibility(View.VISIBLE);
        TorrentSpeedTestTask task = new TorrentSpeedTestTask();
        runningTask = task;
        task.execute(uri);
        taskCanceler = new TaskCanceler(task);
        handler.postDelayed(taskCanceler, TASK_CANCEL_THRESHOLD);
    }

    private void startSpeedtest() {
        view.setText("Performing speedtest...");
        mProgressBar.setVisibility(View.VISIBLE);
        String downloadFileUrl = "http://download.cdn.viber.com/cdn/desktop/Linux/Viber.zip";
        SpeedTestTask task = new SpeedTestTask();
        runningTask = task;
        task.execute(downloadFileUrl);
        taskCanceler = new TaskCanceler(task);
        handler.postDelayed(taskCanceler, TASK_CANCEL_THRESHOLD);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    class SpeedTestTask extends AsyncTask<String, Void, Double> {
        URL url;
        HttpURLConnection conn;
        long totalBytes = 0;
        long downloadTime = 1;
        long start;
        double fastestBlock = 0.0;

        @Override
        protected Double doInBackground(String... params) {
            try {
                url = new URL(params[0]);
                conn = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                start = System.currentTimeMillis();

                long blockSize = BYTE_LIMIT_THRESHOLD/50;
                int currentByte;
                totalBytes = 0;

                long currentblock=0;
                long blockStart = System.currentTimeMillis();
                while ((currentByte = in.read()) != -1) {
                    totalBytes++;
                    currentblock++;
                    if ((totalBytes%1000 == 0)) {
                        publishProgress();

                        if (currentblock > blockSize){
                            double speed = currentblock /
                                    (System.currentTimeMillis() - blockStart);
                            if (speed > fastestBlock) fastestBlock = speed;

                            currentblock = 0;
                            blockStart = System.currentTimeMillis();
                        }

                        if (totalBytes>=BYTE_LIMIT_THRESHOLD)
                            handler.post(taskCanceler);
                    }
                }

                downloadTime = System.currentTimeMillis() - start;
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.v(LOG_TAG, "bytes: " + totalBytes + " time: " + downloadTime);
            return ((double)  totalBytes / downloadTime);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            downloadTime = System.currentTimeMillis() - start;
            mProgressBar.setProgress(mProgressBar.getProgress()+1);
            view.setText("Current download speed: " + String.format("%1.3f", (double) totalBytes / downloadTime) + " kilobytes per second");
        }

        @Override
        protected void onCancelled() {
            Log.v(LOG_TAG, "download cancelled");
            super.onCancelled();
            downloadTime = System.currentTimeMillis() - start;
            view.setText("Calculated download speed: " + String.format("%1.3f", (double) totalBytes / downloadTime) + " kilobytes per second"
                    +"\n Fastest block: "+fastestBlock+" kbps");
            mProgressBar.incrementProgressBy(1);
            mProgressBar.setVisibility(View.GONE);
            SnackbarManager.show(
                    Snackbar.with(SpeedTestActivity.this).text("Speedtest complete!")
                            .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                            .actionLabel("close")
                            .actionColor(Color.WHITE));
            if(taskCanceler != null && handler != null) {
                handler.removeCallbacks(taskCanceler);
            }
        }

        @Override
        protected void onPostExecute(Double kbps) {
            super.onPostExecute(kbps);
            view.setText("Calculated download speed: " + String.format("%1.3f", kbps) + " kilobytes per second"
                    +"\n Fastest block: "+fastestBlock+" kbps");
            mProgressBar.incrementProgressBy(1);
            mProgressBar.setVisibility(View.GONE);
            SnackbarManager.show(
                    Snackbar.with(SpeedTestActivity.this).text("Speedtest complete!")
                            .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                            .actionLabel("close")
                            .actionColor(Color.WHITE));
        }
    }

    class TorrentSpeedTestTask extends AsyncTask<String, Double, Double> {
        long totalBytes = 0;
        long downloadTime = 1;
        double fastestBlock = 0.0;
        long start;
        File torrentLocation;
        Client client;

        @Override
        protected Double doInBackground(String... params) {
            torrentLocation = new File(params[0]);
            start = System.currentTimeMillis();
            try {
                Log.v(LOG_TAG, ".torrent is at: " + torrentLocation);
                Log.v(LOG_TAG, "torrent download will be at: " + torrentDestination);

                long blockSize = BYTE_LIMIT_THRESHOLD/50;

                client = new Client(
                        InetAddress.getLocalHost(),
                        SharedTorrent.fromFile(torrentLocation, torrentDestination));
                start = System.currentTimeMillis();
                client.download();
                Log.v(LOG_TAG, "download started!");
                mProgressBar.setVisibility(View.VISIBLE);

                long currentblock=0;
                long blockStart = System.currentTimeMillis();

                while(client.getState()!= Client.ClientState.DONE){
                    long newBytes = client.getTorrent().getDownloaded();
                    currentblock=currentblock + (newBytes - totalBytes);
                    if (currentblock > blockSize){
                        double speed = currentblock /
                                (System.currentTimeMillis() - blockStart);
                        if (speed > fastestBlock) fastestBlock = speed;

                        currentblock = 0;
                        blockStart = System.currentTimeMillis();
                    }

                    totalBytes = newBytes;
                    downloadTime = System.currentTimeMillis() - start;
                    publishProgress((double) totalBytes / downloadTime);
                    try {
                        if (totalBytes>=BYTE_LIMIT_THRESHOLD)
                            handler.post(taskCanceler);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Log.v(LOG_TAG, "InterruptedException " + e.getMessage());
                        if (client!=null) client.stop(false);
                        return (double) totalBytes / downloadTime;
                    }
                }
                Log.v(LOG_TAG, "done! "+client.getTorrent().getCompletion()+"%");
            } catch (UnknownHostException e) {
                Log.v(LOG_TAG, "UnknownHostException " + e.getMessage());
            } catch (IOException e) {
                Log.v(LOG_TAG, "IOException " + e.getMessage());
            }
            totalBytes = client.getTorrent().getDownloaded();
            downloadTime = System.currentTimeMillis() - start;
            return (double) (totalBytes / downloadTime);
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            if (values[0]==0.0)
                view.setText("Starting...");
            else
                view.setText("Current download speed: " + String.format("%1.3f", values[0]) + " kilobytes per second");
        }

        @Override
            protected void onCancelled() {
            Log.v(LOG_TAG, "download cancelled");
            super.onCancelled();
            if (client!=null) client.stop(false);
            view.setText("Calculated download speed: " + String.format("%1.3f", (double) totalBytes / downloadTime) + " kilobytes per second"
            +"\n Fastest block: "+fastestBlock+" kbps");
            mProgressBar.setVisibility(View.GONE);
            SnackbarManager.show(
                    Snackbar.with(SpeedTestActivity.this).text("Speedtest complete!")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));

            if(taskCanceler != null && handler != null) {
                handler.removeCallbacks(taskCanceler);
            }
            cleanCache(torrentDestination);
        }

        @Override
        protected void onPostExecute(Double kbps) {
            super.onPostExecute(kbps);
            Log.v(LOG_TAG, "speed: " + kbps);
            view.setText("Calculated download speed: " + String.format("%1.3f", (double) totalBytes / downloadTime) + " kilobytes per second"
                    +"\n Fastest block: "+fastestBlock+" kbps");
            mProgressBar.setVisibility(View.GONE);
            SnackbarManager.show(
                    Snackbar.with(SpeedTestActivity.this)
                            .text("Speedtest complete!")
                            .actionLabel("close")
                            .actionColor(Color.WHITE));


            if(taskCanceler != null && handler != null) {
                handler.removeCallbacks(taskCanceler);
            }
            cleanCache(torrentDestination);
        }
    }

    public class TaskCanceler implements Runnable{
        private AsyncTask task;

        public TaskCanceler(AsyncTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            if (task.getStatus() == AsyncTask.Status.RUNNING )
                task.cancel(true);
        }
    }
}

