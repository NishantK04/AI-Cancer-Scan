# app.py
from flask import Flask, request, jsonify
from flask_cors import CORS
from chatbot_logic import get_response

app = Flask(__name__)
CORS(app)                           # allow requests from the Android app

@app.route("/ask", methods=["POST"])
def ask():
    data = request.get_json(silent=True) or {}
    user_msg = data.get("message", "")
    reply = get_response(user_msg)
    return jsonify({"reply": reply})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5003, debug=False)
