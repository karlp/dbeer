package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.*;

public class WhereBeerActivity extends ListActivity {

    public static final String TAG = "WhereBeerActivity";
    private TextView tvStatus;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wherebeer);

        tvStatus = (TextView) findViewById(R.id.where_status);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // First off, use the cached location, to get something up and running...
        // If either of them are too old, or too inaccurate, toss them..
        Location cLocNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location cLocGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Date d = new Date();
        if (isViable(cLocGPS, d)) {
            makeUseOfNewLocation(cLocGPS);
        } else if (isViable(cLocNetwork, d)) {
            makeUseOfNewLocation(cLocNetwork);
        } else {
            // oh well, nothing viable...  FIXME - should update this periodically, letting them know we're still trying...
            tvStatus.setText(R.string.where_beer_location_search);
        }

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
        // Register for both!
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        // emulator has gps, but no network?!
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private boolean isViable(Location location, Date d) {
        if (location == null) {
            return false;
        }
        if (location.getAccuracy() > 1500) {
            Log.d(TAG, "Tossing out location due to poor accuracy:" + location);
            return false;
        }
        if (location.getTime() < d.getTime() - 60 * 60 * 1000) {
            Log.d(TAG, "Tossing out location due to old age:" + location);
            return false;
        }
        return true;
    }

    private void makeUseOfNewLocation(Location location) {
        String xmlr;
        String host = "tera.beeroclock.net";
//        String host = "192.168.149.34:5000";

        String uu = "http://" + host + "/nearest.xml/10?lat=" + location.getLatitude() + "&lon=" + location.getLongitude();

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uu);
        try {
            xmlr = client.execute(request, new BasicResponseHandler());
        } catch (IOException e) {
            // FIXME - this is not really very pretty...
            tvStatus.setText(R.string.where_beer_http_error);
            Log.e(TAG, "Http connection error: " + e.getMessage(), e);
            return;
        }
        Set<Bar> bars = Utils.parseBarXml(xmlr);
        BarArrayAdapter arrayAdapter = new BarArrayAdapter(this, R.layout.where_row_item, new ArrayList<Bar>(bars));
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
        String s = this.getResources().getString(R.string.where_beer_last_update);
        tvStatus.setText(s + " " + new Date());
    }

    public class BarArrayAdapter extends ArrayAdapter<Bar> {
        private Context context;
        private ArrayList<Bar> items;

        public BarArrayAdapter(Context context, int textViewResourceId, ArrayList<Bar> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.items = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            // karl - no idea what this is for yet, comes from http://stackoverflow.com/questions/2265661/how-to-use-arrayadaptermyclass
            // I believe this is if we somehow are in a state where the view is being requested, but has been gc'd, perhaps returning to this activity?
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.where_row_item, null);
            }

            Bar bar = items.get(position);

            if (bar != null) {
                TextView distanceView = (TextView) view.findViewById(R.id.bar_distance);
                if (distanceView != null) {
                    distanceView.setText(String.format("%4.1fm", bar.distance));
                }
                TextView nameView = (TextView) view.findViewById(R.id.bar_name);
                if (nameView != null) {
                    nameView.setText(bar.name);
                }
                TextView priceView = (TextView) view.findViewById(R.id.bar_price);
                if (priceView != null) {
                    // FIXME - should really use per user preferences on what they consider a price of note.
                    priceView.setText(String.valueOf(bar.prices.iterator().next().avgPrice));
                }
            }

            return view;
        }
    }
}
