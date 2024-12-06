from flask import Flask, jsonify
from http import HTTPStatus
import random
import time

app = Flask(__name__)

PORT = 5000
HOST = "0.0.0.0"
FAIL_RATE = 0.2
SLEEP_TIME = 0.7

@app.route("/api", methods=["GET"])
def api_endpoint():
    time.sleep(SLEEP_TIME)
    if random.random() < FAIL_RATE:
        return "Request failed", HTTPStatus.SERVICE_UNAVAILABLE
    return "Success", HTTPStatus.OK

if __name__ == "__main__":
    app.run(host=HOST, port=PORT)
