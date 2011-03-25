from flask import Flask
#import settings

app = Flask("dbeer-services")
app.debug = True

#app.config.from_object('blog.settings')
# TODO - rename this to views or something...
import run_wsgi