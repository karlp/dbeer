package net.beeroclock;

import android.app.Application;

import java.util.Collection;
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
}
