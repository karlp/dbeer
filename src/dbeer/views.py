#!/usr/bin/env python
# coding=utf-8
# Expose our bar finder as a wsgi app


__author__="karl"
__date__ ="$Mar 15, 2011 7:24:21 PM$"

import logging
log = logging.getLogger("dbeer.views")

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

db = models.Db()
db.verify_or_create()

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
    """
    TODO - this should _reallly_ support gzipped uploads!
    """
    db.add_file(request.files['osmfile'])
    return "OK"

@app.route('/nearest.json/<int:num>')
def bars_nearest_json(num=3):
    return bars_nearest(num, tjson=True)

@app.route('/nearest.xml/<int:num>')
def bars_nearest_xml(num=3):
    return bars_nearest(num, txml=True)

@app.route('/bar/<int:barid>.xml', methods=['GET'])
@print_timing
def bar_detail(barid):
    ts = time.time()
    bar = db.bar_by_id(barid)
    tt = time.time() - ts
    log.debug("bar fetch took %f", tt)
    if bar is None:
        abort(404)

    prices = get_avg_prices(bar)
    return Response(render_template("bar.xml", bar=bar, prices=prices), content_type="application/xml; charset=utf-8", )

@app.route('/bar/<int:barid>.xml', methods=['POST', 'PUT'])
def bar_add_price(barid):

    ## Make sure we have this bar in the datastore?
    bar = db.bar_by_id(barid)
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
    orig_date = datetime.utcfromtimestamp(float(orig_date) / 1000)

    user = request.form.get("userid", "None")
    remote_host = request.remote_addr
    if remote_host == '127.0.0.1':
        remote_host = request.headers.get("X-Forwarded-For", "unknown tunneled")
    log.debug("Adding price %s for bar %s on %s from %s@%s", pp, bar, orig_date, user, remote_host)
    db.add_price(bar, pp, price_type, orig_date, float(lat), float(lon), remote_host, request.user_agent.string, user)
    return "OK"

def get_avg_prices(bar):
    """
    Look up the full set of price history for this bar, and squish down to averages by drink type
    """
    return db.avg_prices_for_bar(bar.pkuid)

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

    nearest = db.nearest_bars(lat, lon, num)
    results = []
    # TODO - we should return a nicer datatype here, with "links" to more info...
    # look at the rest media types docs you have
    for i,v in enumerate(nearest):
        results.append({"bar" : v,
                # danger! FIXME - this recalculates the distance!
                "distance" : v.distance(lat,lon),
                "prices" : get_avg_prices(v)
                })

    if tjson:
        return Response(json.dumps(results, default=models.Bar.to_json), content_type="application/javascript; charset=utf-8")
    if txml:
        return Response(render_template("bars.xml", bars=results), content_type="application/xml; charset=utf-8", )
