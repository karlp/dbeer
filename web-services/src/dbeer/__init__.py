from flask import Flask
import models
import logging

log = logging.getLogger("dbeer.app")

config = {
    'results_limit' : 20
}

app = Flask("dbeer-services")
app.debug = True

files = [ "europe.pubsandfriends.osm.001",
    "europe.pubsandfriends.osm.002",
    "europe.pubsandfriends.osm.003",
    "europe.pubsandfriends.osm.004",
    "europe.pubsandfriends.osm.005",
]

import views