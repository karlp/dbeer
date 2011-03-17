package net.beeroclock;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

public class WhereBeerActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        TextView tv = new TextView(this);
//        tv.setText("Hello Android");
        setContentView(R.layout.wherebeer);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        List l = locationManager.getAllProviders();
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        // Register the listener with the Location Manager to receive location updates
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        // emulator has gps, but no network?!
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void makeUseOfNewLocation(Location location) {
        TextView tv = (TextView) findViewById(R.id.tv_wherebeer);
//        tv.append(location.toString());
        URL u;
        String xmlr;
        //String host = "tera.beeroclock.net";
        String host = "192.168.149.34:5000";

        String uu = "http://" + host + "/nearest.xml/5?lat=" + location.getLatitude() + "&lon=" + location.getLongitude();

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uu);
        String content;
        try {
             xmlr = client.execute(request, new BasicResponseHandler());
        } catch (IOException e) {
            throw new IllegalStateException("hatred", e);
        }

        Set<Bar> bars = Utils.parseBarXml(xmlr);
        // great, now what?
        Bar next = bars.iterator().next();
        tv.append("\r\nFound bars, closest is " + next.name + ", which is " + next.distance + "m away");
    }

}
