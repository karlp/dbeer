#!/usr/bin/env python
# Expose our bar finder as a wsgi app

__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import json
import logging

import bottle
from bottle import route, run, request, response, abort

import models

config = {
    'results_limit' : 20
}


logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s",
#filename="/var/log/karlnet_pachube.log"
)
log = logging.getLogger("main")



@route('/nearest/:num#[0-9]+#')
def bars_nearest(num=3):
    lat = request.GET.get("lat")
    lon = request.GET.get("lon")
    if lat is None or lon is None:
        log.debug("Ignoring request without location")
        # TODO - this should raise a custom error page?
        return

    num = int(num)
    # TODO - ensure these are still valid...
    lat = float(lat)
    lon = float(lon)
    if (num > config['results_limit']):
        log.debug("Squashing request for %d bars down to %d", num, config['results_limit'])
        num = config['results_limit']

    # TODO - we actually want to return the distance here too, not jus the bars
    nearest = sorted(od.bars, key=lambda b: b.distance((lon, lat)))
    return json.dumps(nearest[:num], default=models.Bar.to_json)

@route('/hello')
def wopwop():
    return "hi there!"



file = ("../iceland.pubsandfriends.osm")
od = models.OSMData(filename = file)  ## should only load it once..

if __name__ == "__main__":
    bottle.app().catchall = False
    run()
else:
    application = bottle.default_app()
