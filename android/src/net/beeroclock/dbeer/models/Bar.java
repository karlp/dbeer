package net.beeroclock.dbeer.models;

import android.location.Location;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides our client side model of a bar
 * @author karl
 * Date: 3/16/11
 * Time: 8:58 PM
 */
public class Bar implements Comparable<Bar> {
    public String name;
    public long pkuid;
    public Long osmid;
    public String type;
    public double lat;
    public double lon;
    public Double distance;  // The whole idea of this distance being reliable is INSANE!
    public Set<Price> prices = new TreeSet<Price>();

    public static final String PKUID = "pkuid";

    public Bar() {
    }

    public Bar(String name, String type, double lat, double lon, long pkuid) {
        this.name = name;
        this.type = type;
        this.lon = lon;
        this.lat = lat;
        this.pkuid = pkuid;
    }

    // Compare on the physical aspects of the bar, ie, not on distance or price details, which change
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(name);
        hcb.append(type);
        hcb.append(pkuid);
        hcb.append(lat);
        hcb.append(lon);
        return hcb.toHashCode();
    }

    // Compare on the physical aspects of the bar, ie, not on distance or price details, which change
    @Override
    public boolean equals(Object o) {
        if (o instanceof Bar) {
            Bar that = (Bar) o;
            EqualsBuilder eb = new EqualsBuilder();
            eb.append(this.name, that.name);
            eb.append(this.type, that.type);
            eb.append(this.pkuid, that.pkuid);
            eb.append(this.lat, that.lat);
            eb.append(this.lon, that.lon);
            return eb.isEquals();
        }
        return false;
    }

    /**
     * Compare on the physical aspects of the bar, ie, not on distance or price details, which change
     * @param bar the other bar
     * @return you know, what compareTo normally does!
     */
    public int compareTo(Bar bar) {
        CompareToBuilder ctb = new CompareToBuilder();
        ctb.append(this.name, bar.name);
        ctb.append(this.type, bar.type);
        ctb.append(this.pkuid, bar.pkuid);
        ctb.append(this.lat, bar.lat);
        ctb.append(this.lon, bar.lon);
        return ctb.toComparison();
    }

    @Override
    public String toString() {
        return "Bar{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", pkuid=" + pkuid +
                ", osmid=" + osmid +
                ", lat=" + lat +
                ", lon=" + lon +
                ", distance=" + distance +
                ", prices=" + prices +
                '}';
    }

    /**
     * A Comparator to sort _only_ on distance. DO NOT use this to sort things in a set,
     * you'll get very unhappy results, (like throwing out anything that has the same distance)
     * @return a distance only sorter for bars
     */
    public static Comparator<Bar> makeDistanceComparator() {
        return new Comparator<Bar>() {
            public int compare(Bar bar, Bar bar1) {
                return bar.distance.compareTo(bar1.distance);
            }
        };
    }

    public Location toLocation() {
        Location l = new Location("bar.auto");
        l.setLatitude(this.lat);
        l.setLongitude(this.lon);
        return l;
    }
}
