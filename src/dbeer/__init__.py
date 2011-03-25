from flask import Flask
#import settings

app = Flask("dbeer-services")
app.debug = True

#app.config.from_object('blog.settings')
import views