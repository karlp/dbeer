#!/usr/bin/env python
#coding=utf-8
import math

__author__="karl"
__date__ ="$Mar 15, 2011 12:02:57 PM$"

import sys
import models

olstofan_loc = (-21.9309694, 64.1455718)
celtic_loc = (-21.9303364, 64.1465577)
zimsen_loc = (-21.9372481, 64.1478206)

if __name__ == "__main__":

    if len(sys.argv) > 1:
        file = (sys.argv[1])
    else:
        file = ("../iceland.pubsandfriends.osm")
    od = models.OSMData(filename = file)

    for bar in od.bars:
        print "the distance to ", bar, " is:  ", bar.distance(zimsen_loc)

    nearest10 = sorted(od.bars, key=lambda b: b.distance(zimsen_loc))[:10]
    print nearest10