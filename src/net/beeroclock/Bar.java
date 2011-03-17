package net.beeroclock;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: karl
 * Date: 3/16/11
 * Time: 8:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class Bar implements Comparable<Bar> {
    public String name;
    public long osmid;
    public double lat;
    public double lon;
    public Double distance;
    public Set<Price> prices = new TreeSet<Price>();

    public Bar() {
    }

    public Bar(String name, double lat, double lon, long osmid) {
        this.name = name;
        this.lon = lon;
        this.lat = lat;
        this.osmid = osmid;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(name);
        hcb.append(osmid);
        hcb.append(lat);
        hcb.append(lon);
        hcb.append(distance);
        return hcb.toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Bar) {
            Bar that = (Bar) o;
            EqualsBuilder eb = new EqualsBuilder();
            eb.append(this.name, that.name);
            eb.append(this.osmid, that.osmid);
            eb.append(this.lat, that.lat);
            eb.append(this.lon, that.lon);
            eb.append(this.distance, that.distance);
            return eb.isEquals();
        }
        return false;
    }

    /**
     * Sorts on distance first!
     * @param bar the other bar
     * @return you know, what compareTo normally does!
     */
    public int compareTo(Bar bar) {
        CompareToBuilder ctb = new CompareToBuilder();
        ctb.append(this.distance, bar.distance);
        ctb.append(this.name, bar.name);
        ctb.append(this.osmid, bar.osmid);
        ctb.append(this.lat, bar.lat);
        ctb.append(this.lon, bar.lon);
        // XXX skip the prices?
        return ctb.toComparison();
    }

    @Override
    public String toString() {
        return "Bar{" +
                "name='" + name + '\'' +
                ", osmid=" + osmid +
                ", lat=" + lat +
                ", lon=" + lon +
                ", distance=" + distance +
                ", prices=" + prices +
                '}';
    }
}
