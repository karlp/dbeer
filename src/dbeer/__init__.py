from flask import Flask

app = Flask("dbeer-services")
app.debug = True

import views