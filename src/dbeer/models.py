#!/usr/bin/env python
# Karl Palsson, 2011
# Uses an OSM filtered dump of just bars, nightclubs, pubs, cafes and
# restraunts to return the nearest X of them, based on a location...
# oh yes, this does lots of calculations....

import math
import decimal
import logging
log = logging.getLogger("dbeer.models")

from google.appengine.ext import db
import pyosm

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

class Pricing(db.Model):
    bar_osm_id = db.IntegerProperty(required=True)
    location = db.GeoPtProperty(required=True)
    drink_type = db.IntegerProperty(required=True)
    price = DecimalProperty(required=True)
    report_date = db.DateTimeProperty(required=True)

    def __repr__(self):
        return "Pricing(bar=%d, location=%s, type=%s, price=%f, date=%s)" % (self.bar_osm_id, self.location, self.drink_type, self.price, self.report_date)


class Bar:
    """
    Just use locations as a lat/long tuple for now, and use haversines for now
    """
    def __init__(self, name, geo=None, osmid=None, type=None):
        self.name = name
        self.location_geo = geo
        self.osmid = osmid
        self.type = type

    def distance(self, somewhere_geo):
        """
        Use the haversine formula to work out the distance to another geographical point
        """
        return self._hdistance(self.location_geo, somewhere_geo)

    def _hdistance(self, p1, p2):
        """
        From http://www.movable-type.co.uk/scripts/latlong.html
        """
        R = 6371 * 1000
        lat1 = math.radians(p1[1])
        lat2 = math.radians(p2[1])
        dLong = math.radians(p2[0] - p1[0])
        dLat = lat2 - lat1
        a = math.sin(dLat/2) * math.sin(dLat/2) + math.cos(lat1) * math.cos(lat2) * math.sin(dLong/2) * math.sin(dLong/2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
        return R * c

    def __repr__(self):
        return "Bar(name=%s, location=%s, type=%s)" % (self.name, self.location_geo, self.type)

    @staticmethod
    def to_json(pyobj):
        if isinstance(pyobj, Bar):
            return {"name" : pyobj.name, "location" : pyobj.location_geo, "osmid": pyobj.osmid, "type" : pyobj.type}
        return JSONEncoder.default(pyobj)


class OSMData():
    bars = []

    def __init__(self, filename=None):
        if filename is not None:
            self.add_file(filename)

    def add_files(self, files):
        for file in files:
            self.add_file(file)

    def add_file(self, filename):
        log.debug("Starting to parse osm dump: %s", filename)
        try:
            osm = pyosm.OSMXMLFile(filename=filename)
        except IOError, exx:
            log.error("EPIC FAIL: couldn't parse the osm file %s" % exx)
            return
        log.debug("Loaded osm dump")

        ignored_bars = 0
        for barn in osm.nodes.values():
            if 'name' not in barn.tags:
                ignored_bars += 1
            else:
                bar = Bar(barn.tags['name'], geo=(float(barn.lon),  float(barn.lat)), osmid=barn.id, type=barn.tags['amenity'])
                self.bars.append(bar)
        log.debug("loaded %d bars, ignored %d that had no name", len(osm.nodes), ignored_bars)

    def by_osmid(self, osmid):
        for barn in self.bars:
            if barn.osmid == osmid:
                return barn
        return None
