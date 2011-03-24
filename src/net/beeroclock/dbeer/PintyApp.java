package net.beeroclock.dbeer;

import android.app.Application;
import android.location.Location;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.models.Price;
import net.beeroclock.dbeer.models.PricingReport;

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
public class PintyApp extends Application {

    // Probably should become a map, or at least provide ways of getting certain bars back out again...
    private Set<Bar> knownBars;
    private Location lastLocation;
    // These are really a double array of constants, that match the remote server.
    // but android's resource model only gives us single arrays.
    public ArrayList<String> drinkNames;
    public ArrayList<Long> drinkExternalIds;

    public PintyApp() {
        this.knownBars = new TreeSet<Bar>();
    }

    public Set<Bar> getKnownBars() {
        return knownBars;
    }

    public Bar getBar(Long barId) {
        for (Bar b : knownBars) {
            if (b.osmid == barId) {
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

    // simplistically handle updating our local price averages...
    public void addPricingReport(PricingReport report) {
        Bar b = getBar(report.barOsmId);
        Price pp = findPrice(b.prices, report.drinkExternalId);
        if (pp == null) {
            pp = new Price(report.drinkExternalId, report.priceInLocalCurrency.doubleValue());
            b.prices.add(pp);
        } else {
            // simplistic average...
            pp.avgPrice += report.priceInLocalCurrency.doubleValue();
            pp.avgPrice /= 2;
        }
    }

    private Price findPrice(Set<Price> prices, long drinkExternalId) {
        for (Price p : prices) {
            if (p.id == drinkExternalId) {
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
    public String getDrinkNameForExternalId(long id) {
        int i = drinkExternalIds.indexOf(id);
        if (i == -1) {
            return null;
        }
        return this.drinkNames.get(i);
    }
}