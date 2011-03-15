#!/usr/bin/env python
#coding=utf-8
import math
# To change this template, choose Tools | Templates
# and open the template in the editor.

__author__="karl"
__date__ ="$Mar 15, 2011 12:02:57 PM$"

import sys
from models import Bar
import pyosm

olstofan_loc = (-21.9309694, 64.1455718)
celtic_loc = (-21.9303364, 64.1465577)
zimsen_loc = (-21.9372481, 64.1478206)

olstofan = Bar("Ölstofan", geo=olstofan_loc)
zimsen = Bar("Zimsen", zimsen_loc)
celtic = Bar("Celtic Cross", celtic_loc, osmid=123123)


if __name__ == "__main__":

    if len(sys.argv) > 1:
        file = (sys.argv[1])
    else:
        file = ("../iceland.osm.pubsandfriends")

    file = ("../iceland.osm.pubsandfriends")
    #osm = pyosm.OSMXMLFile(filename=None, content=open(file).read())
    osm = pyosm.OSMXMLFile(filename=file)
    #osm.statistic()
    bars = []
    #for barn in osm.nodes.values():
    for barn in osm.nodes:
        if 'name' not in barn.tags:
            print "ignoring bar with no name: ", barn
        else:
            bar = Bar(barn.tags['name'].encode("utf-8"), geo=(float(barn.lon),  float(barn.lat)), osmid=barn.id)
            print "the distance to ", bar, " is:  ", bar.distance(zimsen_loc)

    
