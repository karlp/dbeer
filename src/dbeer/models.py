#!/usr/bin/env python
# Karl Palsson, 2011
# Uses an OSM filtered dump of just bars, nightclubs, pubs, cafes and
# restraunts to return the nearest X of them, based on a location...
# oh yes, this does lots of calculations....

import math
import decimal
import logging
import time
log = logging.getLogger("dbeer.models")

from google.appengine.ext import db
import pyosm

BUCKET_SIZE = 18
R = 6371000

class DecimalProperty(db.Property):
    """
    From: http://googleappengine.blogspot.com/2009/07/writing-custom-property-classes.html
    """
    data_type = decimal.Decimal

    def get_value_for_datastore(self, model_instance):
        return str(super(DecimalProperty, self).get_value_for_datastore(model_instance))

    def make_value_from_datastore(self, value):
        return decimal.Decimal(value)

    def validate(self, value):
        value = super(DecimalProperty, self).validate(value)
        if value is None or isinstance(value, decimal.Decimal):
            return value
        elif isinstance(value, basestring):
            return decimal.Decimal(value)
        raise db.BadValueError("Property %s must be a Decimal or string." % self.name)

class Bar(db.Model):
    name = db.StringProperty(required=True)
    type = db.StringProperty()
    # we allow people to add bars that are not in OSM
    bar_osm_id = db.IntegerProperty()
    lat = db.FloatProperty(required=True)
    lon = db.FloatProperty(required=True)
    # bucketize longitude into X Degree segments.
    lon_bucket = db.IntegerProperty(required=True)

    def distance(self, p2lat, p2lon):
        """
        Use the haversine formula to work out the distance to another geographical point
        From http://www.movable-type.co.uk/scripts/latlong.html
        """
        lat1 = math.radians(self.lat)
        lat2 = math.radians(p2lat)
        dLong = math.radians(p2lon - self.lon)
        dLat = lat2 - lat1
        a = math.sin(dLat/2) * math.sin(dLat/2) + math.cos(lat1) * math.cos(lat2) * math.sin(dLong/2) * math.sin(dLong/2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
        return R * c

    def __repr__(self):
        return "Bar(name=%s, lat=%f, lon=%f, type=%s)" % (self.name, self.lat, self.lon, self.type)

    @staticmethod
    def to_json(pyobj):
        if isinstance(pyobj, Bar):
            return {"name" : pyobj.name, "lat" : pyobj.lat, "lon": pyobj.lon, "osmid": pyobj.bar_osm_id, "type" : pyobj.type}
        return JSONEncoder.default(pyobj)

    @staticmethod
    def add_file(filename):
        """
        Parse an osm dump file, and load it into the data store, updating or creating as need be
        """
        log.debug("Starting to parse osm dump: %s", filename)
        try:
            osm = pyosm.OSMXMLFile(filename=filename)
        except IOError, exx:
            log.error("EPIC FAIL: couldn't parse the osm file %s" % exx)
            return
        log.debug("Loaded osm dump")

        ignored_bars = 0
        new_bars = []
        for barn in osm.nodes.values():
            if 'name' not in barn.tags or barn.tags["name"] == "":
                ignored_bars += 1
            else:
                # Always updated, but use a key to make sure that we never make a duplicate.
                # this _may_ cause some problems with ODBL? it makes it more of a derivative than a collection?
                # Using my own key, and looking for bars by osm id first works too, but it's much much much slower....
                ts = time.time()
                bar = Bar(key_name = "dbeer_%d" % barn.id,
                    name = barn.tags["name"],
                    lat = float(barn.lat),
                    lon = float(barn.lon),
                    lon_bucket = int(round(float(barn.lon) / BUCKET_SIZE)),
                    bar_osm_id = barn.id,
                    type = barn.tags["amenity"])
                new_bars.append(bar)

        ts = time.time()
        db.put(new_bars)
        tt = time.time() - ts
        log.debug("took %f to put %d new", tt, len(new_bars))
        log.info("loaded %d bars, ignored %d nameless, created/updated %d bars", len(osm.nodes), ignored_bars, len(new_bars))

    @staticmethod
    def by_osmid(osmid):
        return db.GqlQuery("select from Bar where bar_osm_id = :1", osmid).get()
        #bar =  db.GqlQuery("select from Bar where bar_osm_id = :1", osmid).get()
        #log.debug("bar key is %s", bar.key())
        #return bar

class Pricing(db.Model):
    """
    We don't need to search pricings based on geo data, so it can just be a normal model
    """
    bar = db.ReferenceProperty(Bar)
    location = db.GeoPtProperty(required=True)
    drink_type = db.IntegerProperty(required=True)
    price = DecimalProperty(required=True)
    report_date = db.DateTimeProperty(required=True)

    def __repr__(self):
        return "Pricing(bar=%d, location=%s, type=%s, price=%f, date=%s)" % (self.bar, self.location, self.drink_type, self.price, self.report_date)

