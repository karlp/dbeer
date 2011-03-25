#!/usr/bin/env python
# coding=utf-8
# Run our app using flask/weurkdseasfsd directly.

__author__="Karl Palsson"
__date__ ="$Mar 25, 2011 3:45:04 PM$"

from dbeer import app

if __name__ == "__main__":
    app.run("0.0.0.0")
