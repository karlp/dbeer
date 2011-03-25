from flask import Flask
import models

def load_files():
    file = ("iceland.pubsandfriends.osm")
    return models.OSMData(filename = file)  ## should only load it once..

config = {
    'results_limit' : 20
}

app = Flask("dbeer-services")
app.debug = True

od = load_files()

import views