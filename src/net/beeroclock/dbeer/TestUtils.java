package net.beeroclock.dbeer;

import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.models.Price;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: karl
 * Date: 3/16/11
 * Time: 9:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestUtils {

    String sampleResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<bars> \n" +
            "    <bar lat=\"64.1287825\" lon=\"-21.8663741\" osmid=\"315158528\"> \n" +
            "        <name>Billiardbarinn</name> \n" +
            "        <distance>1143.8291309</distance> \n" +
            "        <prices><price drinkid=\"1\">550</price></prices> \n" +
            "    </bar> \n" +
            "    <bar lat=\"64.1311429\" lon=\"-21.8678835\" osmid=\"298914063\"> \n" +
            "        <name>Te &amp; Kaffi</name> \n" +
            "        <distance>1386.26126992</distance> \n" +
            "        <prices><price drinkid=\"1\">800</price></prices> \n" +
            "    </bar> \n" +
            "    <bar lat=\"64.1052642\" lon=\"-21.8150646\" osmid=\"326362757\"> \n" +
            "        <name>Café Bar 8</name> \n" +
            "        <distance>2468.63996778</distance> \n" +
            "        <prices><price drinkid=\"1\">800</price></prices> \n" +
            "    </bar> \n" +
            "    <bar lat=\"64.1378744\" lon=\"-21.9134875\" osmid=\"971164355\"> \n" +
            "        <name>Kjarvalsstaðir</name> \n" +
            "        <distance>3593.47360026</distance> \n" +
            "        <prices><price drinkid=\"1\">750</price></prices> \n" +
            "    </bar> \n" +
            "    <bar lat=\"64.1460071\" lon=\"-21.9012007\" osmid=\"815740255\"> \n" +
            "        <name>Amokka Kaffihús</name> \n" +
            "        <distance>3695.38346754</distance> \n" +
            "        <prices><price drinkid=\"1\">550</price></prices> \n" +
            "    </bar> \n" +
            "    </bars>";
    @Test
    public void testParseBarXml() throws Exception {
        Set<Bar> bars = Utils.parseBarXml(sampleResponse);
        assertEquals("Should get all 5 bars", 5, bars.size());
        Bar teOgKaffi = new Bar("Te & Kaffi", 64.1311429, -21.8678835, 298914063L);
        teOgKaffi.distance = 1386.26126992;
        Set<Price> prices = new TreeSet<Price>();
        prices.add(new Price(1, 800));
        teOgKaffi.prices = prices;
        assertEquals("te & kaffi should decode properly", teOgKaffi, bars.toArray(new Bar[bars.size()])[1]);
        assertEquals("names should be encoded properly", "Kjarvalsstaðir", bars.toArray(new Bar[bars.size()])[3].name);
    }

    @Test
    public void testValid_but_irrelevant_XML() throws Exception {
        Set<Bar> bars = Utils.parseBarXml("<gunk/>");
        // TODO - or do we?
        assertEquals("We don't really want to fail, we just want to return nothing?", 0, bars.size());
    }

    @Test
    public void testTotalGarbage() throws Exception {
        Set<Bar> bars = Utils.parseBarXml("total garbage!");
        // TODO - or do we?
        assertEquals("We don't really want to fail, we just want to return nothing?", 0, bars.size());
    }

}
