package net.beeroclock.dbeer.models;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Information to send to the server about a pricing
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-03-24
 */
public class PricingReport {
    public long barOsmId;
    /**
     * The latitude where you were when you reported the price, used to sanity check price updates
     */
    public double lat;
    /**
     * The longitude where you were when you reported the price, used to sanity check price updates
     */
    public double lon;
    public int drinkTypeId;
    public BigDecimal priceInLocalCurrency;

    /**
     * The date the pricing report was created, not necessarily the time it was submitted! (offline, etc)
     */
    public Date dateRecorded;

    public PricingReport(long barOsmId, double lat, double lon, int drinkExternalId, BigDecimal priceInLocalCurrency) {
        this.barOsmId = barOsmId;
        this.lat = lat;
        this.lon = lon;
        this.drinkTypeId = drinkExternalId;
        this.priceInLocalCurrency = priceInLocalCurrency;
        dateRecorded = new Date();
    }
}
