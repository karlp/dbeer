package net.beeroclock.dbeer.activities;

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
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import net.beeroclock.dbeer.Utils;
import net.beeroclock.dbeer.models.Price;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.*;

public class WhereBeerActivity extends ListActivity implements LocationListener {

    public static final String TAG = "WhereBeerActivity";
    public static final int SERVER_DEFAULT_ID = 0;
    public static final int SERVER_DEV_ID = 1;
    public static final int SERVER_CUSTOM_ID = 2;
    public static final int SERVER_NEW_ID = 3;
    private TextView tvStatus;
    private ImageView headerImage;
    PintyApp pinty;
    LocationManager locationManager;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int MENU_DRINKS_ID = DELETE_ID + 1;
    private static final int MENU_SERVER_ID = MENU_DRINKS_ID + 1;
    private ArrayList<Bar> currentlyDisplayedBars;

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
//        Debug.stopMethodTracing();
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, BarDetailActivity.class);
        Bar b = (Bar) v.getTag(R.id.tag_bar);
        i.putExtra(Bar.PKUID, b.pkuid);
        startActivity(i);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Bar toRemove = currentlyDisplayedBars.get((int) info.id);
                pinty.hideBar(toRemove);
                redrawBarList();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE, DELETE_ID, Menu.NONE, R.string.where_beer_ctx_hide_bar);
	}

    /**
     * Handle the menu options for this activity
     * @param item the menu item clicked
     * @return see super()
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_where_hidden_bars:
                Log.d(TAG, "jumping to hidden bars...");
                // TODO
                return true;
            case R.id.menu_where_preferences:
                Intent i = new Intent(this, MyPreferencesActivity.class);
                startActivity(i);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_where, menu);
        return true;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Debug.startMethodTracing("wherebeer");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wherebeer);
        pinty = (PintyApp)getApplication();
        tvStatus = (TextView) findViewById(R.id.where_status);
        headerImage = (ImageView) findViewById(R.id.where_image);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        // Register ourselves for any sort of location update
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);
        tvStatus.setText(R.string.where_beer_location_search);

        // Parse raw drink options... may need to push this to a background task...
        String[] drink_names = getResources().getStringArray(R.array.drink_type_names);
        String[] drink_ids = getResources().getStringArray(R.array.drink_type_values);
        pinty.drinkNames = new ArrayList<String>();
        pinty.drinkExternalIds = new ArrayList<Integer>();
        for (int i = 0; i < drink_names.length; i++) {
            pinty.drinkExternalIds.add(Integer.parseInt(drink_ids[i]));
            pinty.drinkNames.add(drink_names[i]);
        }

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // do we already know sort of where we are?
        // if we do, make sure to update our display!
        Set<Bar> allwedBars = pinty.getAllowedBars();
        if (pinty.getLastLocation() != null && !allwedBars.isEmpty()) {
            Log.i(TAG, "resuming and reusing cached location and bars");
            displayBarsForLocation(pinty.getLastLocation(), allwedBars);
            return;
        }

        // if not,  work out where we are, and try to update our list of bars!
        Location cLocNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location cLocGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (isBetterLocation(cLocGPS, null)) {
            useGoodNewLocation(cLocGPS);
        } else if (isBetterLocation(cLocNetwork, cLocGPS)) {
            useGoodNewLocation(cLocNetwork);
        } else {
            // oh well, nothing viable...  FIXME - should update this periodically, letting them know we're still trying...
            // Can we even do that? we're registered for updates, that's about as good as we can do!
            tvStatus.setText(R.string.where_beer_location_search);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(this);
        // keep pinty.knownBars though, we can fetch it again if we are killed, and there's no reason to remove bars once we learn of them....
    }

    public void onLocationChanged(Location location) {
        // TODO - Need to have some sort of decision on whether this is a useful location or not?
        // no point in continually fetching web resources?
        // As long as we're good enough at maintaining our local state, shouldn't be any reason to refetch, unless we've actually moved....
        // if our app has been killed, we'll get the first one here, which _will_ update
        // android isn't very good really at only sending us updates when it's moved more than 5m.
        // ...at least with the network provider, works for the gps provider
        Location lastLocation = pinty.getLastLocation();
        if (isBetterLocation(location, lastLocation)) {
            useGoodNewLocation(location);
        } else {
            Log.i(TAG, "ignoring new location, it's not as good as the old one: " + location);
        }
    }

    /**
     * Straight from google docs...
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     * Determines whether one Location reading is better than the current Location fix
     * (Added a check for new location == null)
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     * @return true if the new location should be considered "better"
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (location == null) {
            // no location is never worthwhile
            return false;
        }

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 2 * DateUtils.MINUTE_IN_MILLIS;
        boolean isSignificantlyOlder = timeDelta < -2 * DateUtils.MINUTE_IN_MILLIS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = StringUtils.equals(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
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

            HttpClient client = new DefaultHttpClient();
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("lat", String.valueOf(location.getLatitude())));
            qparams.add(new BasicNameValuePair("lon", String.valueOf(location.getLongitude())));
            URI uri;
            try {
                uri = URIUtils.createURI("http", pinty.getServer(), -1, "/nearest.xml/10", URLEncodedUtils.format(qparams, "UTF-8"), null);
            } catch (URISyntaxException e) {
                Log.e(TAG, "how did this happen?!", e);
                throw new IllegalStateException("You shouldn't be able to get here!", e);
            }
            HttpGet request = new HttpGet(uri);

            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (HttpResponseException e) {
                // TODO - should invalidate the position somehow, so it gets refetched?
                Log.e(TAG, e.getStatusCode() + "-" + e.getMessage(), e);
                return new TreeSet<Bar>();
            } catch (IOException e) {
                // FIXME - this is not really very pretty...
                // FIXME - how to notify the user here?
                Log.e(TAG, "Crazy error" + e.getMessage(), e);
                return new TreeSet<Bar>();
            }
            return Utils.parseBarXml(xmlr);
        }

        @Override
        protected void onPostExecute(Set<Bar> bars) {
            super.onPostExecute(bars);
            headerImage.setImageDrawable(getResources().getDrawable(R.drawable.emo_im_happy));

            // TODO - Could add proximity alerts here for each bar?
            // save the bars to our application's current set of bars...
            // TODO Remember, we have to resort the set of bars, based on our current location!
            // Still want a set, so I don't get duplicate bars, but probably should resort it a lot more often
            pinty.getKnownBars().addAll(bars);
            redrawBarList();
        }

    }

    private void redrawBarList() {
        displayBarsForLocation(pinty.getLastLocation(), pinty.getAllowedBars());
    }

    /**
     * Fill the list with the given set of bars, as if we were at the given location.
     * TODO distance in each bar object might not be up to date?
     * @param location "here"
     * @param bars the bars to display, assumed to be in increasing order of distance from "here"
     */
    private void displayBarsForLocation(Location location, Set<Bar> bars) {
        currentlyDisplayedBars = new ArrayList<Bar>(bars);
        // This is probably bogus, it doesn't update the distance based on here, just sorts on where they were...
        // (Which gets updated every time we get a web request, so that's ok?
        Collections.sort(currentlyDisplayedBars, Bar.makeDistanceComparator());
        BarArrayAdapter arrayAdapter = new BarArrayAdapter(this, R.layout.where_row_item, location, currentlyDisplayedBars);
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
        String s = getResources().getString(R.string.where_beer_last_update);
        tvStatus.setText(s + " " + new Date());
    }


    private void useGoodNewLocation(Location location) {
        Log.i(TAG, "updating location and fetching for:" + location);
        pinty.setLastLocation(location);

        new BarServiceFetcher().execute(location);
    }

    public class BarArrayAdapter extends ArrayAdapter<Bar> {
        private Context context;
        private ArrayList<Bar> items;
        private Location here;
        private NumberFormat integerInstance;

        public BarArrayAdapter(Context context, int textViewResourceId, Location here, ArrayList<Bar> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.items = objects;
            this.here = here;
            integerInstance = NumberFormat.getIntegerInstance();
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
                    // Don't use string format on android, just truncate distance to whole meters
                    // TODO - could use a custom number formatter to display km instead of m?  (geeky overload)
                    distanceView.setText(integerInstance.format(bar.distance));
                }
                TextView nameView = (TextView) view.findViewById(R.id.bar_name);
                if (nameView != null) {
                    nameView.setText(bar.name);
                }
                TextView priceView = (TextView) view.findViewById(R.id.bar_price);
                if (priceView != null) {
                    int desiredPriceType = pinty.getFavouriteDrink();
                    boolean done = false;
                    for (Price p : bar.prices) {
                        if (p.drinkTypeId == desiredPriceType) {
                            priceView.setText(String.valueOf(p.avgPrice));
                            done = true;
                        }
                    }
                    if (!done) {
                        priceView.setText("???");
                    }
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

    /**
     * Create a drawable pointed in the heading from "here" to the bar.
     * Ignores the orientation of the device.
     * This is actually not the performance hog you'd think.  Tested with traceview...
     * @param here where we are
     * @param bar the bar of interest
     * @return a correctly oriented iamge
     */
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
