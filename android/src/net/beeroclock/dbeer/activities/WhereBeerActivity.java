package net.beeroclock.dbeer.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import net.beeroclock.dbeer.Utils;
import net.beeroclock.dbeer.models.Price;
import net.beeroclock.dbeer.ws.BarServiceFetcherResult;
import org.acra.ErrorReporter;
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
import org.apache.http.params.CoreProtocolPNames;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;

public class WhereBeerActivity extends ListActivity implements LocationListener, SensorEventListener {

    public static final String TAG = "WhereBeerActivity";
    PintyApp pinty;
    LocationManager locationManager;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int DIALOG_HELP = 1;
    private static final int DIALOG_ABOUT = 2;
    private static final int DIALOG_NO_BARS = 3;
    private ProgressDialog lostDialog;
    BarArrayAdapter arrayAdapter;
    private float mCurrentOrientation;
    private SensorManager sensorManager;
    private Sensor oSensor;
    private Handler uiHandler;
    private static final int MSG_TYPE_ROTATE = 99;
    private AdView adView;

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, BarDetailActivity.class);
        i.putExtra(Bar.PKUID, arrayAdapter.getItem(position).pkuid);
        startActivity(i);
    }

    // This handles the long click event itself
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Bar toRemove = arrayAdapter.getItem((int) info.id);
                arrayAdapter.remove(toRemove);
                pinty.hideBar(toRemove);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    // This is the menu shown for a long click
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
                Intent intentHiddenBars = new Intent(this, HiddenBarEditActivity.class);
                startActivity(intentHiddenBars);
                return true;
            case R.id.menu_where_preferences:
                Intent intentPrefs = new Intent(this, MyPreferencesActivity.class);
                startActivity(intentPrefs);
                return true;
            case R.id.menu_where_help:
                showDialog(DIALOG_HELP);
                return true;
            case R.id.menu_where_about:
                showDialog(DIALOG_ABOUT);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_HELP:
                return builder.setMessage(R.string.where_beer_help_text)
                        .setTitle(R.string.dialog_help_title)
                        .setPositiveButton(R.string.btn_help_view_online_manual, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PintyApp.APP_HELP_PAGE)));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dismissDialog(DIALOG_HELP);
                            }
                        })
                        .create();
            case DIALOG_ABOUT:
                return builder.setTitle(R.string.app_name)
                        .setMessage(R.string.app_about_text)
                        .setPositiveButton(R.string.btn_about_visit_website, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PintyApp.APP_HOME_PAGE)));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dismissDialog(DIALOG_ABOUT);
                            }
                        })
                        .create();
            case DIALOG_NO_BARS:
                return builder.setTitle(R.string.dialog_no_bars_title)
                        .setMessage(R.string.dialog_no_bars_message)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dismissDialog(DIALOG_NO_BARS);
                            }
                        })
                        .create();
        }
        return super.onCreateDialog(id);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.wherebeer);
        pinty = (PintyApp)getApplication();

        uiHandler = new Handler();

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        // Can this ever fail?
        oSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        // Parse raw drink options... may need to push this to a background task...
        String[] drink_names = getResources().getStringArray(R.array.drink_type_names);
        String[] drink_ids = getResources().getStringArray(R.array.drink_type_values);
        pinty.drinkNames = new ArrayList<String>();
        pinty.drinkExternalIds = new ArrayList<Integer>();
        for (int i = 0; i < drink_names.length; i++) {
            pinty.drinkExternalIds.add(Integer.parseInt(drink_ids[i]));
            pinty.drinkNames.add(drink_names[i]);
        }

        // load up current hidden bars...
        pinty.loadHiddenBars();
        arrayAdapter = new BarArrayAdapter(this, R.layout.where_row_item, null, new ArrayList<Bar>());
        setListAdapter(arrayAdapter);

        registerForContextMenu(getListView());
    }

    @Override
    protected void onResume() {
        super.onResume();
        adView = (AdView)this.findViewById(R.id.where_beer_ad_view);
        AdRequest adRequest = new AdRequest();
        Location lastLocation = pinty.getLastLocation();
        if (lastLocation != null) {
            adRequest.setLocation(lastLocation);
        }
        adRequest.setTesting(pinty.ads_test_mode);
        adView.setAdListener(new AdListener() {
            public void onReceiveAd(Ad ad) {
                adView.setVisibility(View.VISIBLE);
            }
            public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) { }

            public void onPresentScreen(Ad ad) { }

            public void onDismissScreen(Ad ad) { }

            public void onLeaveApplication(Ad ad) { }
        });
        adView.loadAd(adRequest);

        // Register ourselves for any sort of location update
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);

        // Also try SENSOR_DELAY_UI, it might be good enough
        sensorManager.registerListener(this, oSensor, SensorManager.SENSOR_DELAY_UI);

        // do we already know sort of where we are?
        // if we do, make sure to update our display!
        Set<Bar> allowedBars = pinty.getAllowedBars();
        if (!isOldLocation(lastLocation) && !allowedBars.isEmpty()) {
            Log.i(TAG, "resuming and reusing cached location and bars");
            displayBarsForLocation(pinty.getLastLocation(), allowedBars);
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
            lostDialog = ProgressDialog.show(this, getResources().getString(R.string.dialog_lost_title),
                    getResources().getString(R.string.dialog_lost_message), true, true);
        }
    }

    /**
     * Is this location worth using for anything?
     * @param lastLocation a location object we're considering using
     * @return true if the location is out of date or null
     */
    private static boolean isOldLocation(Location lastLocation) {
        if (lastLocation == null) {
            return true;
        }
        Date now = new Date();
        return (now.getTime() - lastLocation.getTime() > 3 * DateUtils.MINUTE_IN_MILLIS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Make sure we stop requesting location updates, or we'll drain the battery needlessly
        locationManager.removeUpdates(this);
        // keep pinty.knownBars though, we can fetch it again if we are killed, and there's no reason to remove bars once we learn of them....

        // make sure we stop listening to the orientation sensor too, also for battery saving..
        sensorManager.unregisterListener(this);
    }

    public void onLocationChanged(Location location) {
        // TODO - Need to have some sort of decision on whether this is a useful location or not?
        // no point in continually fetching web resources?
        // As long as we're good enough at maintaining our local state, shouldn't be any reason to refetch, unless we've actually moved....
        // if our app has been killed, we'll get the first one here, which _will_ update
        // android isn't very good really at only sending us updates when it's moved more than 5m.
        // ...at least with the network provider, works for the gps provider
        if (lostDialog != null) {
            lostDialog.dismiss();
        }
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

        if (isOldLocation(location) && location.hasAccuracy()) {
            // an old location is definitely worse.
            // emulator hack... it only has a "gps" and it doesn't support accuracy, and reports totally crazy times
            return false;
        }
        if (currentBestLocation == null) {
            // if the new location is fresh, and the other is null, it must be ok
            return true;
        }

        // below here, both fixes are viable, need to choose the better one...
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 3 * DateUtils.MINUTE_IN_MILLIS;
        boolean isSignificantlyOlder = timeDelta < -3 * DateUtils.MINUTE_IN_MILLIS;
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

    // Update our local field holding our orientation.
    public void onSensorChanged(SensorEvent event) {
        // we get _lots_ of events here, let's chill unless it really swings a bit
        if (Math.abs(mCurrentOrientation - event.values[0]) < 4) {
            return;
        }
        mCurrentOrientation = event.values[0];
        if (!pinty.getAllowedBars().isEmpty()) {
            if (!uiHandler.hasMessages(MSG_TYPE_ROTATE)) {
                Message msg = Message.obtain(uiHandler, new Runnable() {
                    public void run() {
                        // FIXME - this _works_ but it causes much mroe work than we need.  we haven't _moved_, just rotated.
                        // no need to reload all the bars and hidden ones and allowed ones and so forth..
//                        redrawBarList();
                        arrayAdapter.notifyDataSetChanged();
                    }
                });
                msg.what = MSG_TYPE_ROTATE;
                uiHandler.sendMessageDelayed(msg, 100);
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    class BarServiceFetcher extends AsyncTask<Location, Void, BarServiceFetcherResult> {
        Location location;

        @Override
        protected BarServiceFetcherResult doInBackground(Location... locations) {
            if (locations.length == 0) {
                return new BarServiceFetcherResult("No locations given to search for");
            }
            if (locations.length > 1) {
                Log.w(TAG, "ignoring request to fetch multiple locations!");
            }
            location = locations[0];
            String xmlr;

            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, pinty.userAgent);
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("lat", String.valueOf(location.getLatitude())));
            qparams.add(new BasicNameValuePair("lon", String.valueOf(location.getLongitude())));
            URI uri;
            try {
                uri = URIUtils.createURI("http", pinty.getServer(), -1, "/nearest.xml/20", URLEncodedUtils.format(qparams, "UTF-8"), null);
            } catch (URISyntaxException e) {
                Log.e(TAG, "how did this happen?!", e);
                return new BarServiceFetcherResult("How did this happen? URI Syntax exception?!", e);
            }
            HttpGet request = new HttpGet(uri);

            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (HttpResponseException e) {
                // TODO - should invalidate the position somehow, so it gets refetched?
                Log.e(TAG, e.getStatusCode() + "-" + e.getMessage(), e);
                ErrorReporter.getInstance().handleException(e);
                return new BarServiceFetcherResult("invalid response from the server: " + e.getStatusCode(), e);
            } catch (IOException e) {
                Log.e(TAG, "Crazy error" + e.getMessage(), e);
                ErrorReporter.getInstance().handleException(e);
                return new BarServiceFetcherResult("Craziness? " + e.getMessage(), e);
            }
            try {
                Set<Bar> bars = Utils.parseBarXml(xmlr);
                return new BarServiceFetcherResult(bars);
            } catch (Exception e) {
                ErrorReporter.getInstance().handleException(e);
                return new BarServiceFetcherResult("Failed to parse the xml reply", e);
            }
        }

        @Override
        protected void onPostExecute(BarServiceFetcherResult result) {
            super.onPostExecute(result);
            setProgressBarIndeterminateVisibility(false);
            if (result.success) {
                // TODO - Could add proximity alerts here for each bar?
                // save the bars to our application's current set of bars...
                // Make sure that new data replaces any existing data for a given bar.
                pinty.getKnownBars().removeAll(result.bars);
                pinty.getKnownBars().addAll(result.bars);
                redrawBarList();
            } else {
                Toast.makeText(WhereBeerActivity.this, result.message, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void redrawBarList() {
        displayBarsForLocation(pinty.getLastLocation(), pinty.getAllowedBars());
    }

    /**
     * Fill the list with the given set of bars, as if we were at the given location.
     * @param location "here"
     * @param bars the bars to display, assumed to be in increasing order of distance from "here"
     */
    private void displayBarsForLocation(Location location, Set<Bar> bars) {
        if (bars.size() == 0) {
            // replace entire list view with a centered blob of text saying no bars nearby?
            // Note: this is only shown if you start up fresh with no bars.  Because we cache bars locally, if you simply
            // move a long long way, we'll still know about the bars at your old location.
            showDialog(DIALOG_NO_BARS);
        }
        arrayAdapter.setNotifyOnChange(false);
        arrayAdapter.here = location;
        arrayAdapter.clear();
        // TODO - recalc could return a sorted set....
        ArrayList<Bar> recalc = recalculateDistances(location,  bars);
        for (Bar b : recalc) {
            arrayAdapter.add(b);
        }
        arrayAdapter.sort(Bar.makeDistanceComparator());
        arrayAdapter.notifyDataSetChanged();
    }

    private ArrayList<Bar> recalculateDistances(Location location, Set<Bar> bars) {
        Log.d(TAG, "recalculating distances...");
        ArrayList<Bar> ret = new ArrayList<Bar>();
        for (Bar b : bars) {
            b.distance = (double) location.distanceTo(b.toLocation());
            ret.add(b);
        }
        return ret;
    }


    private void useGoodNewLocation(Location location) {
        Log.i(TAG, "updating location and fetching for:" + location);
        pinty.setLastLocation(location);
        setProgressBarIndeterminateVisibility(true);
        new BarServiceFetcher().execute(location);
    }

    public class BarArrayAdapter extends ArrayAdapter<Bar> {
        private Context context;
        private Location here;
        private DecimalFormat df = new DecimalFormat("#.00");
        private DecimalFormat metersFormat = new DecimalFormat("##,###m");
        private DecimalFormat milesFormat = new DecimalFormat("#.0m");
        private DecimalFormat feetFormat = new DecimalFormat("@@@ft");

        public BarArrayAdapter(Context context, int textViewResourceId, Location here, ArrayList<Bar> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.here = here;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            // karl - no idea what this is for yet, comes from http://stackoverflow.com/questions/2265661/how-to-use-arrayadaptermyclass
            // I believe this is if we somehow are in a state where the view is being requested, but has been gc'd, perhaps returning to this activity?
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.where_row_item, null);
                holder = new ViewHolder();
                holder.arrowView = (ImageView) view.findViewById(R.id.bar_direction);
                holder.distanceView = (TextView) view.findViewById(R.id.bar_distance);
                holder.nameView = (TextView) view.findViewById(R.id.bar_name);
                holder.priceView = (TextView) view.findViewById(R.id.bar_price);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Bar bar = getItem(position);

            if (bar != null) {
                int distanceInMetres = bar.distance.intValue();
                if (pinty.isMetric()) {
                    holder.distanceView.setText(metersFormat.format(distanceInMetres));
                } else {
                    if (distanceInMetres < PintyApp.THRESHOLD_MILES_TO_FEET) {
                        holder.distanceView.setText(feetFormat.format(distanceInMetres * PintyApp.METRES_TO_FEET));
                    } else {
                        holder.distanceView.setText(milesFormat.format(distanceInMetres * PintyApp.METRES_TO_MILES));
                    }
                }
                holder.nameView.setText(bar.name);

                int desiredPriceType = pinty.getFavouriteDrink();
                boolean done = false;
                for (Price p : bar.prices) {
                    if (p.drinkTypeId == desiredPriceType) {
                        holder.priceView.setText(df.format(p.avgPrice));
                        done = true;
                    }
                }
                if (!done) {
                    holder.priceView.setText("???");
                }

                DirectedArrow da = new DirectedArrow(getDirection(here, bar) - mCurrentOrientation);
                holder.arrowView.setImageDrawable(da);
            }
            return view;
        }
    }

    /**
     * A performance tuning class, as recommended in some "efficient list view" slides from google.
     */
    private static class ViewHolder {
        ImageView arrowView;
        TextView priceView;
        TextView nameView;
        TextView distanceView;
    }


    public class DirectedArrow extends Drawable {
        float currentBearing = 0;
        Paint mPaint = new Paint();
        Path mPath = new Path();

        public DirectedArrow(float currentBearing) {
            this.currentBearing = currentBearing;
            mPath.moveTo(0, -10);
            mPath.lineTo(-4, 10);
            mPath.lineTo(0, 7);
            mPath.lineTo(4, 10);
            mPath.close();
        }

        @Override
        public void draw(Canvas canvas) {
            Paint paint = mPaint;
            canvas.drawColor(Color.TRANSPARENT);
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            canvas.translate(12, 12);
            canvas.rotate(currentBearing);
            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public void setAlpha(int i) {
            mPaint.setAlpha(i);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Return the bearing from here to the bar.
     */
    private static float getDirection(Location here, Bar bar) {
        float[] ret = new float[2];
        Location.distanceBetween(here.getLatitude(), here.getLongitude(), bar.lat, bar.lon, ret);
        return ret[1];
    }
}
