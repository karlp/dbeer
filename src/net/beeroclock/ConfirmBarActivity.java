package net.beeroclock;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.*;

/**
 * Resolving which bar they are in, letting them change it if necessary, and moving on to adding a pricing.
 *
 * @author karl
 *         Date: 3/7/11
 *         Time: 9:18 PM
 */
public class ConfirmBarActivity extends Activity {

    private PintyApp pinty;
    private static final int CHOOSE_BAR = 1;
    private static final int ADD_PRICE = 2;
    private static final String TAG = "ConfirmBarActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.confirm_bar);
        pinty = (PintyApp) getApplication();
//        doStuff();
    }

    @Override
    protected void onStart() {
        super.onStart();    //To change body of overridden methods use File | Settings | File Templates.
        doStuff();
    }


    public void doStuff() {
        Location location = pinty.getLastLocation();
        if (location != null) {
            makeUseOfNewLocation(location);
        } else {
            // oh well, nothing viable...  FIXME - should update this periodically, letting them know we're still trying...
            Toast t = Toast.makeText(this, "No location, not going to let you add any pricing...", Toast.LENGTH_SHORT);
            t.show();
        }
    }

    private void makeUseOfNewLocation(Location here) {
        // look for any bars within say, 30m...
        ArrayList<Bar> nearbyBars = new ArrayList<Bar>();
        for (Bar bar : pinty.getKnownBars()) {
            float v = here.distanceTo(bar.toLocation());
            if (v < 30) {
                nearbyBars.add(bar);
            }
        }
        if (nearbyBars.size() == 0) {
            // say no nearby bars...
            // what are the users options?
            // redirect them to osm to add the bar?  (that's what I want)
            // allow them to report a price anyway, and I just keep it
            // I do _not_ want to get into auto adding data to OSM
            Toast t = Toast.makeText(this, "You don't seem to be in a bar right now?", Toast.LENGTH_SHORT);
            t.show();
            // show the nearest bars list again? (by intenting back to the where beers tab)
            // or do I want to give them an option to add a price if there is a bar within X * 2 m? (for when our radius is off?)
            return;
        }
        if (nearbyBars.size() == 1) {
            handleAddPriceForSingleBar(nearbyBars.get(0));
        }
        if (nearbyBars.size() > 1) {
            handleMultipleNearbyBars(nearbyBars);
        }
    }

    /**
     * We need to sort out which bar they mean to add a pricing for, by kicking off a new activity to choose..
     * @param nearbyBars the list of nearby bars we need to choose from
     */
    private void handleMultipleNearbyBars(ArrayList<Bar> nearbyBars) {
        Log.d(TAG, "More than one nearby bar, need to choose");
        Intent intent = new Intent(this, ChooseCorrectBarActivity.class);
        long[] barIds = new long[nearbyBars.size()];
        for (int i = 0; i < nearbyBars.size(); i++) {
            barIds[i] = nearbyBars.get(i).osmid;
        }
        intent.putExtra(Bar.OSM_ID_SET, barIds);
        startActivity(intent);
    }

    private void handleAddPriceForSingleBar(Bar bar) {
        Toast t = Toast.makeText(this, "You seem to be in " +  bar.name, Toast.LENGTH_SHORT);
        t.show();
        Log.d(TAG, "Adding a price for " + bar);
        Intent i = new Intent(this, AddPricingActivity.class);
        i.putExtra(Bar.OSM_ID, bar.osmid);
        startActivity(i);
    }
}