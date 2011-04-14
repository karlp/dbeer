package net.beeroclock.dbeer;

import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.models.Price;
import net.beeroclock.dbeer.ws.DBeerServiceStatus;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helpers that don't need to be in an android class, so they can be easier to test
 * @author karl
 * Date: 3/16/11
 * Time: 9:05 PM
 */
public class Utils {

    /**
     * Parse a hopefully fairly open ended blob of xml that looks like what the beer service supplies.
     * @param xmlr any valid xml
     * @return a set of bars, if any could be found in the data..
     */
    public static Set<Bar> parseBarXml(String xmlr) {
        return parseBarXml_ugly(xmlr);
    }

    private static Set<Bar> parseBarXml_ugly(String xmlr) {
        // fuck.
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode node = cleaner.clean(xmlr);

        Set<Bar> ret = new TreeSet<Bar>();
        try {
            Object[] nodes = node.evaluateXPath("//bar");
            for (Object baro : nodes) {
                Bar b = new Bar();
                TagNode bar = (TagNode) baro;
                b.lat = Double.parseDouble(bar.getAttributeByName("lat"));
                b.lon = Double.parseDouble(bar.getAttributeByName("lon"));
                String pkuid = bar.getAttributeByName("pkuid");
                if (StringUtils.isEmpty(pkuid)) {
                    // TODO - could make this a custom error, and do something, but do we care?
                    throw new IllegalStateException("server appears to be an incompatible version? doesn't supply pkuid for bars!");
                } else {
                    b.pkuid = Long.parseLong(pkuid);
                }
                String osmid = bar.getAttributeByName("osmid");
                if (!StringUtils.isEmpty(osmid)) {
                    b.osmid = Long.parseLong(osmid);
                }
                b.name = StringEscapeUtils.unescapeXml(bar.evaluateXPath("name/text()")[0].toString());
                b.type = StringEscapeUtils.unescapeXml(bar.evaluateXPath("type/text()")[0].toString());

                StringBuffer  sb = (StringBuffer) bar.evaluateXPath("distance/text()")[0];
                b.distance = Double.valueOf(sb.toString());

                Set<Price> prices = new TreeSet<Price>();
                Object[] priceNodes = bar.evaluateXPath(".//price");
                for (Object priceo : priceNodes) {
                    TagNode price = (TagNode) priceo;
                    Price p = new Price();
                    p.drinkTypeId = Integer.parseInt(price.getAttributeByName("drinkid"));
                    p.sampleSize = Integer.parseInt(price.getAttributeByName("samples"));
                    p.avgPrice = Double.parseDouble(price.getText().toString());
                    prices.add(p);
                }
                b.prices = prices;

                ret.add(b);

            }
        } catch (XPatherException e) {
            throw new IllegalStateException("something went bad with xpath", e);
        }
        return ret;
    }

    public static DBeerServiceStatus parseStatus(String xmlr) {
        HtmlCleaner cleaner = new HtmlCleaner();
        TagNode node = cleaner.clean(xmlr);
        try {
            String date = node.evaluateXPath("//lastUpdate/date/text()")[0].toString();
            return new DBeerServiceStatus(date);
        } catch (XPatherException e) {
            throw new IllegalStateException("something went bad with xpath", e);
        }
    }
}
