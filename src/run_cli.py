#!/usr/bin/env python
# coding=utf8
import math
# To change this template, choose Tools | Templates
# and open the template in the editor.

__author__="karl"
__date__ ="$Mar 15, 2011 12:02:57 PM$"


from models import Bar
from shapely.geometry import Point
from pyproj import Proj

# This will be ugly. How do I automatically pick something valid here?
# no, just keep lat/long on the bars, and use the "current location" to reproj the bars as needed
proj = Proj(proj='utm',zone=27,ellps='WGS84')
#proj = Proj(init="epsg:3785")  # spherical mercator, should work anywhere...
olstofan_loc = (-21.9309694, 64.1455718)
olstofan_proj = proj(*olstofan_loc)
celtic_loc = (-21.9303364, 64.1465577)
celtic_proj = proj(*celtic_loc)
zimsen_loc = (-21.9372481, 64.1478206)
zimsen_proj = proj(*zimsen_loc)

olstofan = Point(olstofan_proj)
celtic = Point(celtic_proj)
zimsen = Point(zimsen_proj)

olstofan = Bar("Ölstofan", geo=olstofan_loc)
zimsen = Bar("Zimsen", zimsen_loc)
celtic = Bar("Celtic Cross", celtic_loc, osmid=123123)

if __name__ == "__main__":
    print "olstafan is at ", olstofan.distance(zimsen_loc)
    print "celtic is at ", celtic.distance(zimsen_loc)

    
