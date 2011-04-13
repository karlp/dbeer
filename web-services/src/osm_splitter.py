#!/usr/bin/env python
# coding=utf-8
# FIXME - fill in some documentation here, maybe a license!
# To change this template, choose Tools | Templates
# and open the template in the editor.

__author__="Karl Palsson, Mar 25, 2011 6:28:21 PM"

import sys
from xml.etree.ElementTree import ElementTree, Element
import logging
log = logging.getLogger(__file__)

def read_file(filename):
    log.debug("About to open file")
    tree = ElementTree(file=filename)
    log.debug("File opened")
    root = tree.getroot()

    file_count = 0
    node_count = 0
    nodes_per_file = 1000
    new_file = None
    head = None
    for node in root:
        if node_count % nodes_per_file == 0:
            if new_file is not None:
                log.debug("writing file: %s", new_file)
                ElementTree(head).write(new_file)
                log.debug("finished file: %s", new_file)
            file_count += 1
            new_file = "%s.%03d" % (filename, file_count)
            head = Element("osm")
            head.attrib["version"] = "0.6"
            head.attrib["generator"] = "osm_splitter.py"

        head.append(node)
        node_count += 1
    log.debug("writing file: %s", new_file)
    ElementTree(head).write(new_file)
    log.debug("finished file: %s", new_file)

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
    file = sys.argv[1]
    log.info("Processing file: %s", file)
    read_file(sys.argv[1])
