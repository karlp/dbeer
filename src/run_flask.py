#!/usr/bin/env python
# coding=utf-8
# Run our app using flask/weurkdseasfsd directly.

__author__="Karl Palsson"
__date__ ="$Mar 25, 2011 3:45:04 PM$"

import logging
logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

from dbeer import app

if __name__ == "__main__":
    app.run("0.0.0.0")
