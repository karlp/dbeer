from flask import Flask
import models
import logging

log = logging.getLogger("dbeer.app")

def load_file(input_od, filename):
    log.info("Loading %s...", filename)
    input_od.add_file(filename)
    log.info("Finished loading %s, now have %d bars", filename, len(input_od.bars))

config = {
    'results_limit' : 20
}

app = Flask("dbeer-services")
app.debug = True

od = models.OSMData()
load_file(od, "europe.pubsandfriends.osm.001")
load_file(od, "europe.pubsandfriends.osm.002")
load_file(od, "europe.pubsandfriends.osm.003")
load_file(od, "europe.pubsandfriends.osm.004")
load_file(od, "europe.pubsandfriends.osm.005")

import views