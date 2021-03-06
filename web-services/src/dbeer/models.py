#!/usr/bin/env python
# Karl Palsson, 2011
# Uses an OSM filtered dump of just bars, nightclubs, pubs, cafes and
# restraunts to return the nearest X of them, based on a location...
# oh yes, this does lots of calculations....

import math
import datetime
import time
import logging
log = logging.getLogger("dbeer.models")

from pyspatialite import dbapi2 as sqlite3
import pyosm

BUCKET_SIZE = 18
R = 6371000
BBOX_SIZE = 0.5

#conn.row_factory = sqlite3.Row

config = {
    'dbfile' : "/home/karl/src/dbeer/web-services/data/dbeer-demo.sqlite",
    'sql_create_geom_file' : "/home/karl/src/dbeer/web-services/data/init_spatialite-2.3.sql",
    'sql_create_tables': "/home/karl/src/dbeer/web-services/src/create_dbeer_db.1.sql",
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
            c.executescript(open(config['sql_create_tables']).read())
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
        add_or_update_nodes(osm.nodes)

    def add_or_update_nodes(self, nodeset, source_file="unknown"):
        ignored_bars = 0
        updated_bars = 0
        new_bars = 0
        conn = sqlite3.connect(config['dbfile'])
        c = conn.cursor()
        update_tstamp = time.time()
        for barn in nodeset.values():
            if 'name' not in barn.tags or barn.tags["name"] == "":
                ignored_bars += 1
            else:
                # Always updated, but use a key to make sure that we never make a duplicate.
                bar = Bar(barn.tags["name"], float(barn.lat), float(barn.lon), type=barn.tags["amenity"], osmid=barn.id)
                cnt = c.execute("select count(1) from bars where osmid = ?", (bar.osmid,)).fetchone()[0]
                if cnt >= 1:
                    c.execute("update bars set name = ?, type =?, updated = ?, geometry = geomFromText('POINT(%f %f)', 4326) where osmid = ?" % (bar.lon, bar.lat),
                        (bar.name, bar.type, update_tstamp, bar.osmid))
                    updated_bars += 1
                else:
                    # oh fuck you very much spatialite
                    c.execute("insert into bars (name, type, osmid, created, geometry) values (?, ?, ?, ?, geomFromText('POINT(%f %f)', 4326))" % (bar.lon, bar.lat),
                        (bar.name, bar.type, bar.osmid, update_tstamp))
                    new_bars += 1

        username = "unknown" # FIXME
        # FIXME - make this log a failure too please!
        c.execute("""insert into data_updates (date, username, bars_created, bars_modified, source_file, status)
                    values (?, ?, ?, ?, ?, ?)""",
                    (update_tstamp, username, new_bars, updated_bars, source_file, "OK"))
        conn.commit()
        conn.close()
        log.info("loaded %d bars, ignored %d nameless, created %d, updated %d", len(nodeset), ignored_bars, new_bars, updated_bars)

    def remove_nodes(self, nodeset, source_file="unknown"):
        """
        Mark bars as removed, presumably because an osm changes file indicated that they were deleted.
        """
        conn = sqlite3.connect(config['dbfile'])
        c = conn.cursor()
        update_tstamp = time.time()
        for barn in nodeset.values():
            c.execute("update bars set deleted = ? where osmid = ?",
                (update_tstamp, barn.id))

        username = "unknown" # FIXME
        c.execute("""insert into data_updates (date, username, bars_removed, source_file, status)
                    values (?, ?, ?, ?, ?)""",
                    (update_tstamp, username, len(nodeset), source_file, "OK"))
        conn.commit()
        conn.close()
        log.info("removed %d bars", len(nodeset))

    def last_update(self):
        """
        Return the timestamp of the last database update
        """
        conn = sqlite3.connect(config['dbfile'])
        conn.row_factory = sqlite3.Row
        row = conn.execute("select date, bars_removed, bars_created, bars_modified from data_updates where date = (select max(date) from data_updates)").fetchone()
        lu = {}
        lu['bars_removed'] = row['bars_removed']
        lu['bars_created'] = row['bars_created']
        lu['bars_modified'] = row['bars_modified']
        lu['date'] = datetime.datetime.fromtimestamp(row['date'])
        #lu['date'] = int(row['date'])
        conn.close()
        return lu

    def bar_by_id(self, barid):
        """
        Look up a bar by our internal ID
        """
        conn = sqlite3.connect(config['dbfile'])
        conn.row_factory = sqlite3.Row
        rows = conn.execute("select pkuid, name, type, osmid, x(geometry) as lon, y(geometry) as lat from bars where pkuid = ?", (barid,)).fetchall()
        if len(rows) == 0:
            return None
        if len(rows) > 1:
            raise Exception("more than one bar with the same PKUID id: %s" % barid)
        bar = Bar(rows[0]['name'], rows[0]['lat'], rows[0]['lon'], type=rows[0]['type'], osmid=rows[0]['osmid'])
        bar.pkuid = rows[0]['pkuid']
        conn.close()
        return bar

    def nearest_bars(self, lat, lon, count, lat_delta=BBOX_SIZE, lon_delta=BBOX_SIZE):
        """
        Get all the bars withing a box centered on a point, then sort those on real distance, and return them in order...
        """
        conn = sqlite3.connect(config['dbfile'])
        conn.row_factory = sqlite3.Row
        bars = []
        rows = conn.execute("select pkuid, name, type, osmid, x(geometry) as lon, y(geometry) as lat from bars where mbrContains(BuildMBR(?, ?, ?, ?), geometry)",
            (lon - lon_delta, lat - lat_delta, lon + lon_delta, lat + lat_delta))
        for row in rows:
            bar = Bar(row['name'], row['lat'], row['lon'], type=row['type'], osmid=row['osmid'])
            bar.pkuid = row['pkuid']
            bars.append(bar)

        # FIXME - this sort does a calulation on the distance, which is then done _again_ to populate the xml results
        # arguably, we don't even need to sort, just return all the bars within the bbox, and their distances, the phone has to sort it again anyway,
        # and will be recalculating the distances all the time anyway
        nearest = sorted(bars, key=lambda b: b.distance(lat, lon))
        return nearest[:count]

    def avg_prices_for_bar(self, bar_pkuid):
        prices = []
        conn = sqlite3.connect(config['dbfile'])
        rows = conn.execute("select drink_type, avg(price), count(price) from pricings where barid = ? group by drink_type", (bar_pkuid,)).fetchall()
        for row in rows:
            prices.append({'drink_type': row[0], 'average': row[1], 'samples': row[2]})
        return prices

    def add_price(self, bar, price, drink_type, orig_date, lat, lon, remote_host, user_agent, userid):
        conn = sqlite3.connect(config['dbfile'])
        conn.execute("""insert into pricings
            (barid, drink_type, price, date, geometry, host, user_agent, userid)
            values (?, ?, ?, ?, geomFromText('point(%f %f)', 4326), ?, ?, ?)""" % (lon, lat),
            (bar.pkuid, drink_type, price, orig_date, remote_host, user_agent, userid))
        conn.commit()

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
            return {"name" : pyobj.name, "pkuid" : pyobj.pkuid, "lat" : pyobj.lat, "lon": pyobj.lon, "osmid": pyobj.osmid, "type" : pyobj.type}
        return JSONEncoder.default(pyobj)
