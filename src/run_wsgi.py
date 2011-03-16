#!/usr/bin/env python
# Expose our bar finder as a wsgi app

__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import json
import logging
import random

from bottle import route, run, request, response, abort, debug

import models

config = {
    'results_limit' : 20
}


logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("main")



@route('/nearest.json/:num#[0-9]+#')
def bars_nearest_json(num=3):
    return bars_nearest(num, tjson=True)

@route('/nearest.xml/:num#[0-9]+#')
def bars_nearest_xml(num=3):
    return bars_nearest(num, txml=True)

def bars_nearest(num=3, tjson=False, txml=False):
    lat = request.GET.get("lat")
    lon = request.GET.get("lon")
    if lat is None or lon is None:
        log.debug("Ignoring request without location")
        abort(500, "No location provided")

    num = int(num)
    try:
        lat = float(lat)
        lon = float(lon)
    except ValueError:
        abort(500, "Invalid lat/lon values %s/%s" % (lat,lon))

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

    if tjson:
        response.content_type = "application/javascript"
        return json.dumps(results, default=models.Bar.to_json)
    if txml:
        response.content_type = "application/xml"
        return "<notjsoan/>"


file = ("../iceland.pubsandfriends.osm")
od = models.OSMData(filename = file)  ## should only load it once..

debug(True)
run(reloader=True)
