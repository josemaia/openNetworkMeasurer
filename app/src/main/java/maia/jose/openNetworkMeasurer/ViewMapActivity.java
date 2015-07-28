package maia.jose.openNetworkMeasurer;

import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import maia.jose.openNetworkMeasurer.values.myPrivateValues;

public class ViewMapActivity extends AppCompatActivity {
    private final String LOG_TAG = ViewMapActivity.class.getSimpleName();
    private boolean geolocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view);
        String url = myPrivateValues.measurementServerLocation;

        WebView myWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        if (isGpsEnabled()){
            webSettings.setGeolocationEnabled(true);
            geolocation = true;}
        else{
            webSettings.setGeolocationEnabled(false);
            geolocation = false;
        }
        myWebView.loadUrl(url);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setWebChromeClient(new GeoWebChromeClient());
    }

    private boolean isGpsEnabled()
    {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER)&&service.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public class GeoWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            callback.invoke(origin, geolocation, false);
        }


    }
}
