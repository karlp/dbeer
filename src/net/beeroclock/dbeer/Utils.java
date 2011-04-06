package net.beeroclock.dbeer;

import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.models.Price;
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
                b.pkuid = Long.parseLong(bar.getAttributeByName("pkuid"));
                String osmid = bar.getAttributeByName("osmid");
                if (!StringUtils.isEmpty(osmid)) {
                    b.osmid = Long.parseLong(osmid);
                }
                b.name = StringEscapeUtils.unescapeXml(bar.evaluateXPath("name/text()")[0].toString());

                StringBuffer  sb = (StringBuffer) bar.evaluateXPath("distance/text()")[0];
                b.distance = Double.valueOf(sb.toString());

                Set<Price> prices = new TreeSet<Price>();
                Object[] priceNodes = bar.evaluateXPath(".//price");
                for (Object priceo : priceNodes) {
                    TagNode price = (TagNode) priceo;
                    Price p = new Price();
                    p.id = Long.parseLong(price.getAttributeByName("drinkid"));
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

}
