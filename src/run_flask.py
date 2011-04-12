#!/usr/bin/env python
# coding=utf-8
# Run our app using flask/weurkdseasfsd directly.

__author__="Karl Palsson"
__date__ ="$Mar 25, 2011 3:45:04 PM$"

import logging
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
logging.getLogger("dbeer").setLevel(logging.DEBUG)

from dbeer import app as application

if __name__ == "__main__":
    application.run("0.0.0.0")

