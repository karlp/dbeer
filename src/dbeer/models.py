#!/usr/bin/env python
# Karl Palsson, 2011
# Uses an OSM filtered dump of just bars, nightclubs, pubs, cafes and
# restraunts to return the nearest X of them, based on a location...
# oh yes, this does lots of calculations....

import math
import logging

import pyosm

log = logging.getLogger("dbeer.models")

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

    def __init__(self, filename):
        log.debug("Starting to parse osm dump")
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

        log.debug("loaded %d bars, ignored %d that had no name", len(self.bars), ignored_bars)

    def by_osmid(self, osmid):
        for barn in self.bars:
            if barn.osmid == osmid:
                return barn
        return None
