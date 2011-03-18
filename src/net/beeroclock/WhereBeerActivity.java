package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        String xmlr;
        //String host = "tera.beeroclock.net";
        String host = "192.168.149.34:5000";

        String uu = "http://" + host + "/nearest.xml/10?lat=" + location.getLatitude() + "&lon=" + location.getLongitude();

        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(uu);
        try {
            xmlr = client.execute(request, new BasicResponseHandler());
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't reach the server... FIXME handle this better");
        }
        Set<Bar> bars = Utils.parseBarXml(xmlr);
        BarArrayAdapter arrayAdapter = new BarArrayAdapter(this, R.layout.where_row_item, new ArrayList<Bar>(bars));
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
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
            }

            return view;
        }
    }
}
