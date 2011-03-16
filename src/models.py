#!/usr/bin/env python
# Karl Palsson, 2011
# Uses an OSM filtered dump of just bars, nightclubs, pubs, cafes and
# restraunts to return the nearest X of them, based on a location...
# oh yes, this does lots of calculations....

import math
import logging

import pyosm


class NullHandler(logging.Handler):
    def emit(self, record):
        pass

h = NullHandler()
logging.getLogger("dbeer.services.models").addHandler(h)

class Bar:
    """
    Just use locations as a lat/long tuple for now, and use haversines for now
    """
    def __init__(self, name, geo=None, osmid=None):
        self.name = name
        self.location_geo = geo
        self.osmid= osmid

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
        return "Bar(name=%s, location=%s)" % (self.name, self.location_geo)

    @staticmethod
    def to_json(pyobj):
        if isinstance(pyobj, Bar):
            return {"name" : pyobj.name, "location" : pyobj.location_geo, "osmid": pyobj.osmid}
        return JSONEncoder.default(pyobj)


class OSMData():
    bars = []

    def __init__(self, filename):
        logging.debug("Starting to parse osm dump")
        osm = pyosm.OSMXMLFile(filename=filename)
        logging.debug("Loaded osm dump")

        ignored_bars = 0
        for barn in osm.nodes.values():
            if 'name' not in barn.tags:
                ignored_bars += 1
            else:
                bar = Bar(unicode(barn.tags['name']), geo=(float(barn.lon),  float(barn.lat)), osmid=barn.id)
                self.bars.append(bar)

        logging.debug("loaded all bars... %d total, ignored %d that had no name", len(self.bars), ignored_bars)
