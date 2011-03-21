#!/usr/bin/env python
# coding=utf-8
# Expose our bar finder as a wsgi app


__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import json
import logging
import random
import time

from flask import Flask, request, abort, Response, render_template
import models

app = Flask(__name__)
config = {
    'results_limit' : 20
}

full_prices = {}

logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("main")

@app.route("/status")
def status():
    return "OK"

@app.route('/nearest.json/<int:num>')
def bars_nearest_json(num=3):
    return bars_nearest(num, tjson=True)

@app.route('/nearest.xml/<int:num>')
def bars_nearest_xml(num=3):
    return bars_nearest(num, txml=True)

@app.route('/bar/<int:osmid>.xml', methods=['GET'])
def bar_detail(osmid):
    bar = od.by_osmid(osmid)
    if bar is None:
        abort(404)

    prices = get_avg_prices(bar)

    return Response(render_template("bar.xml", bar=bar, prices=prices), content_type="application/xml; charset=utf-8", )

# FIXME - test with PUT too
@app.route('/bar/<int:osmid>.xml', methods=['POST', 'PUT'])
def bar_add_price(osmid):
    bar = od.by_osmid(osmid)
    if bar is None:
        abort(404)
    pp = request.form.get('price')
    price_type = request.form.get('price_type', 1) # default to beer.. TODO - is this fair?
    # we want the time from the user, because that will be the _local_ time,
    # which is more relevant for beer pricing than server time.
    orig_date = request.form.get('price_date')
    if pp is None or orig_date is None:
        abort(500, "price and original date are required")

    # FIXME - validate orig_date is a date of some sort...
    # Just use seconds since the epoch?
    log.debug("Adding price %s for bar %s on %s", pp, bar, orig_date)
    add_price(bar, pp, price_type, orig_date)
    return "OK"

def add_price(bar, price, price_type, date):
    """
    This is ok for now, but it's way too hacky for real use.
    Problems:
        Doesn't track numbers of price samples, simply always a moving window of the last averge, plus the new one
        All in memory!
    """
    pp = full_prices.get(bar.osmid)
    if pp is None:
        log.debug("No existing prices for this bar, creating")
        full_prices[bar.osmid] = {}
    this_price = full_prices[bar.osmid].get(price_type)
    if this_price is None:
        # First record of a price for this drink type
        log.debug("first pricing of this type: %s", price_type)
        full_prices[bar.osmid][price_type] = int(price)
    else:
        log.debug("old average: %s, adding new price %s", this_price, price)
        full_prices[bar.osmid][price_type] = (this_price + int(price)) / 2


def get_avg_prices(bar):
    """
    Look up the full set of price history for this bar, and squish down to averages by drink type
    """
    # hehe, this will do for now...
    pp = full_prices.get(bar.osmid)
    if pp is None:
        log.debug("defaulting prices, as we have no current data")
        pp = { 1 : random.randrange(500, 950, 50)}
    return pp

def print_timing(func):
    def wrapper(*arg, **kwarg):
        t1 = time.time()
        res = func(*arg, **kwarg)
        t2 = time.time()
        log.debug("%s took %0.3f ms", func.func_name, (t2-t1)*1000)
        return res
    return wrapper

@print_timing
def bars_nearest(num=3, tjson=False, txml=False):
    lat = request.args.get("lat")
    lon = request.args.get("lon")
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
                "prices" : get_avg_prices(v.osmid)
                })

    if tjson:
        return Response(json.dumps(results, default=models.Bar.to_json), content_type="application/javascript; charset=utf-8")
    if txml:
        return Response(render_template("bars.xml", bars=results), content_type="application/xml; charset=utf-8", )


file = ("../iceland.pubsandfriends.osm")
#file = ("../europe.pubsandfriends.osm")
od = models.OSMData(filename = file)  ## should only load it once..

if __name__ == '__main__':
    app.debug = True
    app.run(host='0.0.0.0')
