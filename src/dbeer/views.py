#!/usr/bin/env python
# coding=utf-8
# Expose our bar finder as a wsgi app


__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import logging
log = logging.getLogger("dbeer.views")

import random
from datetime import datetime
import time

## python 2.5 workarounds (works for GAE 1.4.2+ as well)
try:
    import json
except:
    import simplejson as json

from flask import request, abort, Response, render_template
from dbeer import app, config
import models
from google.appengine.ext import db
from google.appengine.api import quota
from google.appengine.api import memcache

full_prices = {}

def print_timing(func):
    def wrapper(*arg, **kwarg):
        t1 = time.time()
        res = func(*arg, **kwarg)
        t2 = time.time()
        log.debug("%s took %0.3f ms", func.func_name, (t2-t1)*1000)
        return res
    return wrapper

@app.route("/status")
def status():
    return "OK"

@app.route('/upload', methods=['POST'])
def add_raw_dump():
    models.Bar.add_file(request.files['osmfile'])
    return "OK"

@app.route('/nearest.json/<int:num>')
def bars_nearest_json(num=3):
    return bars_nearest(num, tjson=True)

@app.route('/nearest.xml/<int:num>')
def bars_nearest_xml(num=3):
    return bars_nearest(num, txml=True)

@app.route('/bar/<int:osmid>.xml', methods=['GET'])
@print_timing
def bar_detail(osmid):
    ts = time.time()
    bar = models.Bar.by_osmid(osmid)
    tt = time.time() - ts
    log.debug("fetch by osmid took %f", tt)
    if bar is None:
        abort(404)

    prices = get_avg_prices(bar)
    return Response(render_template("bar.xml", bar=bar, prices=prices), content_type="application/xml; charset=utf-8", )

def memget(raw_key):
    key = str(raw_key)
    d = memcache.get(key)
    if d is not None:
        return d
    d = db.get(raw_key)
    memcache.add(key, d, 60)
    return d

# FIXME - test with PUT too
@app.route('/bar/<int:osmid>.xml', methods=['POST', 'PUT'])
def bar_add_price(osmid):

    ## Make sure we have this bar in the datastore?
    bar = models.Bar.by_osmid(osmid)
    if bar is None:
        abort(404)
    pp = request.form.get('price')
    price_type = int(request.form.get('price_type', 1)) # default to beer.. TODO - is this fair?
    # we want the time from the user, because that will be the _local_ time,
    # which is more relevant for beer pricing than server time.
    orig_date = request.form.get('price_date')
    if pp is None or orig_date is None:
        abort(500, "price and price_date are required")

    lat = request.form.get("recordedLat")
    lon = request.form.get("recordedLon")
    orig_date = datetime.utcfromtimestamp(float(orig_date))

    log.debug("Adding price %s for bar %s on %s", pp, osmid, orig_date)
    add_price(bar, pp, price_type, orig_date, lat, lon)
    return "OK"

def add_price(bar, price, price_type, date, lat, lon):
    """
    Just stuff it into the database!
    """
    pp = models.Pricing(bar=bar, location=db.GeoPt(lat, lon), drink_type=price_type, price=price, report_date=date)
    db.put(pp)


def get_avg_prices(bar):
    """
    Look up the full set of price history for this bar, and squish down to averages by drink type
    """
    prices = {}
    for p in bar.pricing_set:
        bleh = prices.get(p.drink_type, [])
        bleh.append(p.price)
        prices[p.drink_type] = bleh

    averages = {}
    for drink_type in prices:
        averages[drink_type] = sum(prices[drink_type]) / len(prices[drink_type])
    return averages

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

    # TODO - this should consider how close to the edge of a lon_bucket we are, and fetch the adjacent one if we're "close"
    ts = time.time()
    slice = models.Bar.all(keys_only=True).filter("lon_bucket =", int(round(lon / models.BUCKET_SIZE))).filter("lat >", lat-1).filter("lat <", lat+1)
    tt = time.time() - ts
    log.debug("slice fetch (keys) took %f", tt)
    #ts = time.time()
    start = quota.get_request_cpu_usage()
    bb = map(memget, slice)
    end = quota.get_request_cpu_usage()
    log.info("_true_ slice fetch (mem) took %d megacycles for %d entries", end - start, len(bb))

    start = quota.get_request_cpu_usage()
    nearest = sorted(bb, key=lambda b: b.distance(lat, lon))
    end = quota.get_request_cpu_usage()
    log.info("slice sort took %d megacycles for %d entries", end - start, len(nearest))

    #tt = time.time() - ts
    #log.debug("slice sort took %f for %d entries", tt, len(nearest))

    results = []
    # TODO - we should return a nicer datatype here, with "links" to more info...
    # look at the rest media types docs you have
    for i,v in enumerate(nearest[:num]):
        results.append({"bar" : v,
                # danger! FIXME - this _recalculates_ the distance!
                "distance" : v.distance(lat,lon),
                "prices" : get_avg_prices(v)
                })

    if tjson:
        return Response(json.dumps(results, default=models.Bar.to_json), content_type="application/javascript; charset=utf-8")
    if txml:
        return Response(render_template("bars.xml", bars=results), content_type="application/xml; charset=utf-8", )
