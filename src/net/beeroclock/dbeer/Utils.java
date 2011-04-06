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
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
//            // we're on froyo+, and we have xpath..
//            return parseBarXml_xpath(xmlr);
//        } else {
            return parseBarXml_ugly(xmlr);
//        }
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


//    private static Set<Bar> parseBarXml_xpath(String xmlr) {
//        XPathFactory pathFactory = XPathFactory.newInstance();
//        XPath xpath = pathFactory.newXPath();
//        InputSource is = new InputSource(new StringReader(xmlr));
//        NodeList nodes;
//        Set<Bar> ret = new TreeSet<Bar>();
//        try {
//            nodes = (NodeList) xpath.evaluate("/bars/bar", is, XPathConstants.NODESET);
//
//            for (int i = 0; i < nodes.getLength(); i++) {
//                Node node = nodes.item(i);
//
//                Bar b = new Bar();
//                b.lat = Double.parseDouble(node.getAttributes().getNamedItem("lat").getTextContent());
//                b.lon = Double.parseDouble(node.getAttributes().getNamedItem("lon").getTextContent());
//                b.pkuid = Long.parseLong(node.getAttributes().getNamedItem("pkuid").getTextContent());
//                String osmid = node.getAttributes().getNamedItem("osmid").getTextContent();
//                if (!StringUtils.isEmpty(osmid)) {
//                    b.osmid = Long.parseLong(osmid);
//                }
//                b.name = (String) xpath.evaluate("name", node, XPathConstants.STRING);
//                b.distance = (Double) xpath.evaluate("distance", node, XPathConstants.NUMBER);
//
//                NodeList priceNodes = (NodeList) xpath.evaluate("prices/price", node, XPathConstants.NODESET);
//                b.prices = parsePriceXml(priceNodes);
//                ret.add(b);
//            }
//        } catch (XPathExpressionException e) {
//            throw new IllegalArgumentException("you got busted xpath somewhere: " + e.getMessage(), e);
//        }
//        return ret;
//    }
//
//    private static Set<Price> parsePriceXml(NodeList nodes) {
//        Set<Price> ret = new TreeSet<Price>();
//        for (int i = 0; i < nodes.getLength(); i++) {
//            Node node = nodes.item(i);
//            Price p = new Price();
//            p.id = Long.parseLong(node.getAttributes().getNamedItem("drinkid").getTextContent());
//            p.avgPrice = Float.parseFloat(node.getTextContent());
//            ret.add(p);
//        }
//        return ret;
//    }

}
