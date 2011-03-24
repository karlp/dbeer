package net.beeroclock.dbeer.models;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents our (android client side) view of a bar's pricing.  Only the type of drink, and the current average.
 * More details stats are server side only
 * @author karl
 * Date: 3/16/11
 * Time: 8:59 PM
 */
public class Price implements Comparable<Price> {
    public long id;
    public double avgPrice;

    public Price() {
    }

    public Price(long id, double avgPrice) {
        this.id = id;
        this.avgPrice = avgPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Price) {
            Price that = (Price)o;
            EqualsBuilder eb = new EqualsBuilder();
            eb.append(this.id, that.id);
            eb.append(this.avgPrice, that.avgPrice);
            return eb.isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hb = new HashCodeBuilder();
        hb.append(this.id);
        hb.append(this.avgPrice);
        return hb.toHashCode();
    }

    public int compareTo(Price that) {
        CompareToBuilder cb = new CompareToBuilder();
        cb.append(this.id, that.id);
        cb.append(this.avgPrice, that.avgPrice);
        return cb.toComparison();
    }

    @Override
    public String toString() {
        return "Price{" +
                "id=" + id +
                ", avgPrice=" + avgPrice +
                '}';
    }
}
