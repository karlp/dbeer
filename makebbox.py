#!/usr/bin/env python
import math

def point_at_distance(start, distance, bearing):
    """
    formula from: http://www.movable-type.co.uk/scripts/latlong.html
var lat2 = Math.asin( Math.sin(lat1)*Math.cos(d/R) + 
                      Math.cos(lat1)*Math.sin(d/R)*Math.cos(brng) );
var lon2 = lon1 + Math.atan2(Math.sin(brng)*Math.sin(d/R)*Math.cos(lat1), 
                             Math.cos(d/R)-Math.sin(lat1)*Math.sin(lat2));
"""
    start_long = math.radians(start[0])
    start_latt = math.radians(start[1])

    ad = distance * 1.0 / 6371  # angular distance
    lat2 = math.asin(math.sin(start_latt) * math.cos(ad) +
                     math.cos(start_latt) * math.sin(ad) * math.cos(bearing))
    long2 = start_long + math.atan2(math.sin(bearing) * math.sin(ad) * math.cos(start_latt),
                                    math.cos(ad) - math.sin(start_latt) * math.sin(lat2));

    return (math.degrees(long2), math.degrees(lat2))

def bbox(start, distance):
    """
    print out the bbox made by moving out distance m in each direction from start
    for OSM, this is left, bottom, right, top, as long, lat, long, lat
    """
    
    return (point_at_distance(start, distance, 270)[0],
        point_at_distance(start, distance, 180)[1],
        point_at_distance(start, distance, 90)[0],
        point_at_distance(start, distance, 0)[1])
        


if __name__ == '__main__':
 home = (-21.9279, 64.1431)
 # use this with: 
 # wget "http://xapi.openstreetmap.org/api/0.6/node[amenity=pub][bbox=-21.935165024804999,64.132332101248991,-21.891040971878855,64.161086432118381]" -O -
 # bbox= bits are straight out of this code...
 print bbox(home, 1)


