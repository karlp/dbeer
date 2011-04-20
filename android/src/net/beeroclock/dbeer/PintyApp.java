package net.beeroclock.dbeer;

import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.models.Price;
import net.beeroclock.dbeer.models.PricingReport;
import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used for holding state across activities within the app.  Notably, keeping the current known list of bars,
 * so we can avoid repeatedly refetching them across the network.  If we ever wanted this to run in any sort of
 * offline mode, we'd have to be keeping this in a db instead, but I prefer to imagine a connected world.
 * @author karl
 * Date: 3/22/11
 * Time: 9:42 AM
 */
@ReportsCrashes(formKey="dHZfRldtSG56NFhTNlg5Q2R1SXlUV1E6MQ",
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.crash_toast_text)
public class PintyApp extends Application {

    public static final String DEFAULT_SERVER = "dbeer-services.ekta.is";
    public static final String PREF_FAVOURITE_DRINK = "favourite_drink";
    public static final String PREF_SERVER = "server";
    public static final String PREF_SHOW_CAFES = "show_cafes";
    public static final String PREF_METRIC = "use_metric";
    public static final int THRESHOLD_MILES_TO_FEET = 160;  // 0.1 miles
    public static final float METRES_TO_MILES = 0.000621371192f;
    public static final double METRES_TO_FEET = 3.2808399;
    // Probably should become a map, or at least provide ways of getting certain bars back out again...
    private Set<Bar> knownBars;
    private Set<Long> hiddenBars;
    private Location lastLocation;
    // These are really a double array of constants, that match the remote server.
    // but android's resource model only gives us single arrays.
    public ArrayList<String> drinkNames;
    public ArrayList<Integer> drinkExternalIds;
    public String userAgent;
    public static final String APP_HOME_PAGE = "http://dbeer.ekta.is";
    public static final String APP_HELP_PAGE = "http://dbeer.ekta.is/screenshots";
    public boolean ads_test_mode = true;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        this.knownBars = new TreeSet<Bar>();
        this.hiddenBars = new TreeSet<Long>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        PackageInfo pi;
        try {
            pi = getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            // yeah right, we can't find ourselves :)
            ErrorReporter.getInstance().handleException(e);
            throw new IllegalStateException(e);
        }
        this.userAgent = String.format("Apache-HttpClient/Android %s (%s %s/%s) %s/%d",
                Build.VERSION.RELEASE, Build.MANUFACTURER, Build.PRODUCT, Build.MODEL, pi.packageName, pi.versionCode);
        ACRA.init(this);
        super.onCreate();
    }

    public Set<Bar> getKnownBars() {
        return knownBars;
    }

    public Bar getBar(Long barId) {
        for (Bar b : knownBars) {
            if (b.pkuid == barId) {
                return b;
            }
        }
        return null;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }

    /**
     * Update our local pricing view, in case we don't get an update from the server anytime soon.
     * @param report the pricing report we're submitting to the server
     */
    public void addPricingReport(PricingReport report) {
        Bar b = getBar(report.barOsmId);
        Price pp = findPrice(b.prices, report.drinkTypeId);
        if (pp == null) {
            pp = new Price(report.drinkTypeId, report.priceInLocalCurrency.doubleValue());
            b.prices.add(pp);
        } else {
            pp.sampleSize++;
            double tmp = pp.avgPrice + report.priceInLocalCurrency.doubleValue();
            pp.avgPrice = tmp / pp.sampleSize;
        }
    }

    private Price findPrice(Set<Price> prices, int drinkExternalId) {
        for (Price p : prices) {
            if (p.drinkTypeId == drinkExternalId) {
                return p;
            }
        }
        return null;
    }

    /**
     * Because we're dealing with these ugly dual arrays, we need a way to pull right thing out of the names based on the foreign key
     * @param id external id of the drink name we're looking for
     * @return drink name
     */
    public String getDrinkNameForExternalId(int id) {
        int i = drinkExternalIds.indexOf(id);
        if (i == -1) {
            return null;
        }
        return this.drinkNames.get(i);
    }

    public void hideBar(Bar toRemove) {
        LocalDatabase db = new LocalDatabase(this);
        ContentValues values = new ContentValues();
        values.put(LocalDatabase.HB_PKUID, toRemove.pkuid);
        values.put(LocalDatabase.HB_NAME, toRemove.name);
        db.getWritableDatabase().insert(LocalDatabase.TABLE_HIDDEN_BARS, null, values);
        hiddenBars.add(toRemove.pkuid);
        db.close();
    }

    public void unhideBar(Bar bar) {
        LocalDatabase db = new LocalDatabase(this);
        db.getWritableDatabase().delete(LocalDatabase.TABLE_HIDDEN_BARS, LocalDatabase.HB_PKUID + "=?", new String[]{String.valueOf(bar.pkuid)});
        hiddenBars.remove(bar.pkuid);
        db.close();
    }

    public Set<Bar> getAllowedBars() {
        Set<Bar> nonHiddenBars = stripByPkuid(knownBars, hiddenBars);
        return stripByPreferences(nonHiddenBars);
    }

    /**
     * Remove all the bars from a set that are of a type we don't want to show.
     * ie, remove all cafes/restaraunts from a list, or all nightclubs for instance.
     * @param bars unfiltered bar set, (will be modified!)
     * @return the same bars, without any bar desired hidden in preferences.
     */
    private Set<Bar> stripByPreferences(Set<Bar> bars) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showCafes = pref.getBoolean(PREF_SHOW_CAFES, true);
        Set<Bar> toHide = new TreeSet<Bar>();
        for (Bar b : bars) {
            if (StringUtils.equals(b.type, "cafe") & !showCafes) {
                toHide.add(b);
            }
        }
        bars.removeAll(toHide);
        return bars;
    }

    private Set<Bar> stripByPkuid(Set<Bar> knownBars, Set<Long> hiddenBars) {
        Set<Bar> ret = new TreeSet<Bar>();
        for (Bar b : knownBars) {
            if (!hiddenBars.contains(b.pkuid)) {
                ret.add(b);
            }
        }
        return ret;
    }

    /**
     * Return the users favourite drink.
     * TODO - android dev notes seem to indicate that you should save this locally, and only commit/read from prefs when the activity is created/closed
     * (probably for performance reasons?)
     * @return the external id of their saved favourite drink
     */
    public int getFavouriteDrink() {
        return Integer.valueOf(sharedPreferences.getString(PREF_FAVOURITE_DRINK, drinkExternalIds.get(0).toString()));
    }

    public String getServer() {
        return sharedPreferences.getString(PREF_SERVER, DEFAULT_SERVER);
    }

    public boolean isMetric() {
        return sharedPreferences.getBoolean(PREF_METRIC, true);
    }

    /**
     * Reload the  list of hidden bars.
     */
    public void loadHiddenBars() {
        hiddenBars.clear();
        SQLiteDatabase db = new LocalDatabase(this).getReadableDatabase();
        Cursor cur = db.query(LocalDatabase.TABLE_HIDDEN_BARS, new String[]{LocalDatabase.HB_PKUID}, null, null, null, null, null);
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            long hiddenBarId = cur.getLong(0);
            Log.d("pinty", "loaded hidden barid: " + hiddenBarId);
            hiddenBars.add(hiddenBarId);
       	    cur.moveToNext();
        }
        cur.close();
        db.close();
    }

    public boolean isAllowed(long pkuid) {
        return !hiddenBars.contains(pkuid);
    }

}
