package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.*;

public class WhereBeerActivity extends ListActivity {

    public static final String TAG = "WhereBeerActivity";
    private TextView tvStatus;
    private Location lastLocation;
    private static final float DISTANCE_JITTER = 30.0f;

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);    //To change body of overridden methods use File | Settings | File Templates.
        Intent i = new Intent(this, BarDetailActivity.class);
        Bar b = (Bar) v.getTag(R.id.tag_bar);
        i.putExtra(Bar.OSM_ID, b.osmid);
        startActivity(i);
    }

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
        if (Utils.isViable(cLocGPS, d)) {
            makeUseOfNewLocation(cLocGPS);
        } else if (Utils.isViable(cLocNetwork, d)) {
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

    class BarServiceFetcher extends AsyncTask<Location, Void, Set<Bar>> {
        Location location;

        @Override
        protected Set<Bar> doInBackground(Location... locations) {
            if (locations.length == 0) {
                return new TreeSet<Bar>();
            }
            if (locations.length > 1) {
                Log.w(TAG, "ignoring request to fetch multiple locations!");
            }
            location = locations[0];
            String xmlr;
            String host = "tera.beeroclock.net";

            Log.i(TAG, "Fetching nearest bar details from: " + host);
            String uu = String.format("http://%s/nearest.xml/%d?lat=%f&lon=%f", host, 10, location.getLatitude(), location.getLongitude());

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(uu);
            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (IOException e) {
                // FIXME - this is not really very pretty...
                // FIXME - how to notify the user here?
                Log.e(TAG, "Http connection error: " + e.getMessage(), e);
                return new TreeSet<Bar>();
            }
            return Utils.parseBarXml(xmlr);
        }

        @Override
        protected void onPostExecute(Set<Bar> bars) {
            super.onPostExecute(bars);
            BarArrayAdapter arrayAdapter = new BarArrayAdapter(WhereBeerActivity.this, R.layout.where_row_item, location, new ArrayList<Bar>(bars));
            ListView lv = getListView();
            lv.setAdapter(arrayAdapter);
            String s = getResources().getString(R.string.where_beer_last_update);
            tvStatus.setText(s + " " + new Date());

            // save the bars to our application's current set of bars...
            PintyApp pinty = (PintyApp)getApplication();
            pinty.getKnownBars().addAll(bars);

            // TODO - Could add proximity alerts here for each bar?
        }
    }

    private void makeUseOfNewLocation(Location location) {
        if (lastLocation != null) {
            float newDelta = lastLocation.distanceTo(location);
            if ( newDelta < DISTANCE_JITTER) {
                // FIXME - this should do some checking on accuracy, and provider of the new location too...
                Log.d(TAG, "Ignoring new location, it's too close to the old one: " + newDelta);
                return;
            }
        }
        lastLocation = location;
        new BarServiceFetcher().execute(location);
    }

    public class BarArrayAdapter extends ArrayAdapter<Bar> {
        private Context context;
        private ArrayList<Bar> items;
        private Location here;

        public BarArrayAdapter(Context context, int textViewResourceId, Location here, ArrayList<Bar> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.items = objects;
            this.here = here;
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
                ImageView arrowView = (ImageView) view.findViewById(R.id.bar_direction);
                if (arrowView != null) {
                    arrowView.setImageDrawable(makeArrowToBar(here, bar));
                }

            }
            view.setTag(R.id.tag_bar, bar);
            return view;
        }
    }

    private Drawable makeArrowToBar(Location here, Bar bar) {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_menu_goto);
        // Getting width & height of the given image.
        int w1 = bmp.getWidth();
        int h1 = bmp.getHeight();
        // Setting post rotate to 90
        Matrix mtx = new Matrix();
        // TODO - this doesn't take into account the current heading of the phone. :(
        mtx.postRotate(getDirection(here, bar) - 135); // image is naturally pointing to 135
        // Rotating Bitmap
        Bitmap rotatedBMP = Bitmap.createBitmap(bmp, 0, 0, w1, h1, mtx, true);
        return new BitmapDrawable(rotatedBMP);
    }

    private float getDirection(Location here, Bar bar) {
        Location l = new Location("local");
        l.setLatitude(bar.lat);
        l.setLongitude(bar.lon);
        return here.bearingTo(l);
    }
}
