#!/usr/bin/env python
# Expose our bar finder as a wsgi app

__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import json
import logging
import random

import bottle
from bottle import route, run, request, response, abort

import models

config = {
    'results_limit' : 20
}


logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
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

    nearest = sorted(od.bars, key=lambda b: b.distance((lon, lat)))
    results = []
    # TODO - we should return a nicer datatype here, with "links" to more info...
    # look at the rest media types docs you have
    for i,v in enumerate(nearest[:num]):
        results.append({"bar" : v,
                "distance" : v.distance((lon,lat)),
                "prices" : { "beer" : random.randrange(500, 950, 50)}})
    return json.dumps(results, default=models.Bar.to_json)

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
