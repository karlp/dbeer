#!/usr/bin/env python
# coding=utf-8
# "main" for google app engine.  This is what app.yaml points to,
# to run our whole shebang.

import logging
from google.appengine.ext.webapp.util import run_wsgi_app

# Neat trick, because all my code is "dbeer.blah" they all inherit from this logger
logging.getLogger("dbeer").setLevel(logging.DEBUG)

from dbeer import app

run_wsgi_app(app)