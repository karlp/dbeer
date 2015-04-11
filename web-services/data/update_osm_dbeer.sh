#!/bin/bash
#
# 2015 closure note: this used to get run from cron
# But stopped due to changes in OSMs dump format in Feb 2012
#
# Karl's script for pulling down the daily changesets and 
# uploading our database.  This should update the planet,
# pulling only ~40-50meg per day.
# Karl Palsson, April 2011

YDAY=`date +%Y%m%d -d yesterday` 
TDAY=`date +%Y%m%d -d today`

#SERVER="http://dbeer.ekta.is"
SERVER="http://planet.openstreetmap.org/daily"
#SERVER="http://ftp.heanet.ie/mirrors/openstreetmap.org/daily"
#SERVER="http://ftp5.gwdg.de/pub/misc/openstreetmap/planet.openstreetmap.org/daily/"
OFILTER=$HOME/bin/osmfilter32
UPDATER=$HOME/src/dbeer/web-services/src/update_from_osm.py
TMPFILE=/tmp/changes.$YDAY-$TDAY.filtered.$$.osc

#set -e
set -o pipefail
wget --progress=dot:mega $SERVER/$YDAY-$TDAY.osc.gz -O - | zcat | $OFILTER --drop-ways --drop-relations | $OFILTER -k"amenity=pub =bar =restaraunt =cafe =nightclub" > $TMPFILE
rc=$?
if [[ $rc != 0 ]]
then
    echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo "+    IT'S ALL BUSTED!!!!                                +"
    echo "+       MAKE SURE YOU RERUN THIS SHIT!!!                +"
    echo "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
    exit $rc
fi
#echo "dothis: curl http://dbeer-services.ekta.is/upload -Fosmfile=@$TMPFILE"
#curl http://dbeer-services.ekta.is/upload -Fosmfile=@$TMPFILE
python $UPDATER $TMPFILE
rm -f $TMPFILE

