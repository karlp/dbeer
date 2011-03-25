#!/usr/bin/env python
# coding=utf-8
# "main" for google app engine.  This is what app.yaml points to,
# to run our whole shebang.

from google.appengine.ext.webapp.util import run_wsgi_app
from dbeer import app

run_wsgi_app(app)