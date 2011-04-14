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
    public int drinkTypeId;
    public double avgPrice;
    public int sampleSize;

    public Price() {
    }

    /**
     * Create an initial pricing report, for a sample size of 1
     * @param drinkTypeId the drink type this price represents
     * @param avgPrice the current average price, ie, the only price
     */
    public Price(int drinkTypeId, double avgPrice) {
        this.drinkTypeId = drinkTypeId;
        this.avgPrice = avgPrice;
        this.sampleSize = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Price) {
            Price that = (Price)o;
            EqualsBuilder eb = new EqualsBuilder();
            eb.append(this.drinkTypeId, that.drinkTypeId);
            eb.append(this.avgPrice, that.avgPrice);
            eb.append(this.sampleSize, that.sampleSize);
            return eb.isEquals();
        }
        return false;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hb = new HashCodeBuilder();
        hb.append(this.drinkTypeId);
        hb.append(this.avgPrice);
        hb.append(this.sampleSize);
        return hb.toHashCode();
    }

    public int compareTo(Price that) {
        CompareToBuilder cb = new CompareToBuilder();
        cb.append(this.drinkTypeId, that.drinkTypeId);
        cb.append(this.avgPrice, that.avgPrice);
        cb.append(this.sampleSize, that.sampleSize);
        return cb.toComparison();
    }

    @Override
    public String toString() {
        return "Price{" +
                "drinkTypeId=" + drinkTypeId +
                ", avgPrice=" + avgPrice +
                ", sampleSize=" + sampleSize +
                '}';
    }
}
