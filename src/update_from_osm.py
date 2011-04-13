#!/usr/bin/env python
# coding=utf-8
# FIXME - fill in some documentation here, maybe a license!
# Takes either an OSM or an OSC (osm changes) file and applies it to our database
# 

__author__="Karl Palsson, Apr 13, 2011 12:32:04 AM"

import logging
log = logging.getLogger(__file__)
import dbeer.models
import pyosm

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
    db = dbeer.models.Db()
    db.verify_or_create()
    import sys
    for filename in sys.argv[1:]:
        ext = filename[-3:]
        if ext == 'osm':
            log.info("<<Parsing %s as a regular OSM file, can only update/create", filename)
            osm = pyosm.OSMXMLFile(filename)
            osm.statistic()
            db.add_or_update_nodes(osm.nodes)
            log.info(">>Completed loading data from %s", filename)
        elif ext == 'osc':
            log.info("<<Parsing %s as a changes file, can do full updates", filename)
            osc = pyosm.OSCXMLFile(filename)
            osc.statistic()
            db.add_or_update_nodes(osc.create_nodes, filename)
            log.info("Finished creating new nodes")
            # Note, you can still "create" here, because we might be adding the name for the first time
            db.add_or_update_nodes(osc.modify_nodes, filename)
            log.info("Finished modifying nodes")
            db.remove_nodes(osc.delete_nodes, filename)
            log.info("Finished deleting nodes")
            log.info(">>Completed loading data from %s", filename)
        else:
            log.warn("Unrecognised file extension (.osm or .osc): %s", filename)

