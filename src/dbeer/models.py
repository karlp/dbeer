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

from pyspatialite import dbapi2 as sqlite3
import pyosm

BUCKET_SIZE = 18
R = 6371000

#conn.row_factory = sqlite3.Row

config = {
    'dbfile' : "/home/karl/src/dbeer-services/dbeer-demo.sqlite",
    'sql_create_table_bars' :
        """CREATE TABLE bars(PKUID integer primary key autoincrement,
                            name text not null,
                            osmid integer,
                            type text,
                            geometry blob not null
                            )""",
    'sql_create_table_pricings' :
        """CREATE TABLE pricings(PKUID integer primary key autoincrement,
                            date integer not null,
                            barid integer not null,
                            drink_type integer not null,
                            geometry blob not null
                            )""",
    'sql_update_geom_bars' : "SELECT RecoverGeometryColumn('bars', 'geometry', 4326, 'POINT', 2)",
    'sql_update_geom_pricings' : "SELECT RecoverGeometryColumn('pricings', 'geometry', 4326, 'POINT', 2)",
    'sql_create_geom_file' : "init_spatialite-2.3.sql",
}

class Db():
    """
    Wraps up the spatialite db holding the bar and price data
    """
    def verify_or_create(self):
        """
        opens, and creates if needed, the spatialite db
        """
        conn = sqlite3.connect(config['dbfile'])
        c = conn.cursor()
        rows = c.execute("select name from sqlite_master where type = 'table' and name = 'bars'")
        if len(rows.fetchall()) == 0:
            log.info("bars table didn't exist, creating everything")
            c.executescript(open(config['sql_create_geom_file']).read())
            c.execute(config['sql_create_table_bars'])
            c.execute(config['sql_create_table_pricings'])
            c.execute(config['sql_update_geom_bars'])
            c.execute(config['sql_update_geom_pricings'])
        else:
            log.info("Bars table existed, assuming everything else is in place")


    def add_file(self, filename):
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
        updated_bars = 0
        conn = sqlite3.connect(config['dbfile'])
        c = conn.cursor()
        for barn in osm.nodes.values():
            if 'name' not in barn.tags or barn.tags["name"] == "":
                ignored_bars += 1
            else:
                # Always updated, but use a key to make sure that we never make a duplicate.
                # this _may_ cause some problems with ODBL? it makes it more of a derivative than a collection?
                # Using my own key, and looking for bars by osm id first works too, but it's much much much slower....
                # oh fuck you very much spatialite
                bar = Bar(barn.tags["name"], float(barn.lat), float(barn.lon), type=barn.tags["amenity"], osmid=barn.id)
                log.debug("inserting bar: %s", bar)
                c.execute("insert into bars (name, type, osmid, geometry) values (?, ?, ?, geomFromText('POINT(%f %f)', 4326))" % (bar.lon, bar.lat),
                    (bar.name, bar.type, bar.osmid))
                updated_bars += 1

        conn.commit()
        log.info("loaded %d bars, ignored %d nameless, created/updated %d bars", len(osm.nodes), ignored_bars, updated_bars)

    def by_osmid(self, osmid):
        conn = sqlite3.connect(config['dbfile'])
        conn.row_factory = sqlite3.Row
        log.debug("looking for osmid: %d", osmid)
        rows = conn.execute("select name, type, osmid, x(geometry) as lon, y(geometry) as lat from bars where osmid = ?", (osmid,)).fetchall()
        if len(rows) == 0:
            return None
        if len(rows) > 1:
            raise Exception("more than one bar with the same OSM id: %s" % osmid)

        bar = Bar(rows[0]['name'], rows[0]['lat'], rows[0]['lon'], type=rows[0]['type'], osmid=rows[0]['osmid'])
        return bar


class Bar():

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

    def __init__(self, name, lat, lon, type=None, osmid=None):
        self.name = name
        self.lat = lat
        self.lon = lon
        self.type = type
        self.osmid = osmid

    def __repr__(self):
        return "Bar(name=%s, lat=%f, lon=%f, type=%s)" % (self.name, self.lat, self.lon, self.type)

    def pricing_set(self):
        return []

    @staticmethod
    def to_json(pyobj):
        if isinstance(pyobj, Bar):
            return {"name" : pyobj.name, "lat" : pyobj.lat, "lon": pyobj.lon, "osmid": pyobj.bar_osm_id, "type" : pyobj.type}
        return JSONEncoder.default(pyobj)


class Pricing():
    """
    We don't need to search pricings based on geo data, so it can just be a normal model
    """
#    bar = db.ReferenceProperty(Bar)
#    location = db.GeoPtProperty(required=True)
#    drink_type = db.IntegerProperty(required=True)
#    price = DecimalProperty(required=True)
#    report_date = db.DateTimeProperty(required=True)

    def __repr__(self):
        return "Pricing(bar=%d, location=%s, type=%s, price=%f, date=%s)" % (self.bar, self.location, self.drink_type, self.price, self.report_date)

