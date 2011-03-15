from imposm.parser import OSMParser

# simple class that handles the parsed OSM data.
class NodesHandler(object):
    def nodes(self, node):
        for osmid, tags, refs in node:
            print tags
            print tags['name'].encode("utf-8")


# instantiate counter and parser and start parsing
counter = NodesHandler()
p = OSMParser(concurrency=4, nodes_callback=counter.nodes)
p.parse("../iceland.pubsandfriends.osm")
