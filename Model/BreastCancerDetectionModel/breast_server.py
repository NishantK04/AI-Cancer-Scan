# breast_server.py
from flask import Flask, request, jsonify, send_file
import os, cv2, numpy as np, tensorflow as tf
from werkzeug.utils import secure_filename

app = Flask(__name__)

# ────────────────────────────── paths ─────────────────────────────
UPLOAD_FOLDER  = 'uploads'
OUTPUT_FOLDER  = 'static'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

# ───────────────────── load breast-cancer model ───────────────────
model              = tf.keras.models.load_model('breast_cancer_mobilenetv2.h5')
LAST_CONV_LAYER    = 'block_16_project_BN'   # MobileNetV2 final conv BN layer
print('[BREAST] model loaded')

# ──────────────────────── helper functions ────────────────────────
def preprocess(img_path, size=(224,224)):
    """Read → RGB → resize → scale [0,1] → add batch dim."""
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, size).astype(np.float32) / 255.0
    return np.expand_dims(img, 0)

def make_heatmap(img_array):
    """Grad-CAM for one-neuron sigmoid output."""
    grad_model = tf.keras.Model(
        inputs=model.inputs,
        outputs=[model.get_layer(LAST_CONV_LAYER).output, model.output]
    )
    with tf.GradientTape() as tape:
        conv_out, preds = grad_model(img_array)
        class_score     = preds[:, 0]          # scalar sigmoid
    grads   = tape.gradient(class_score, conv_out)
    weights = tf.reduce_mean(grads, axis=(0,1,2))
    cam     = tf.squeeze(conv_out[0] @ weights[..., tf.newaxis])
    cam     = tf.maximum(cam, 0) / tf.math.reduce_max(cam)
    return cam.numpy()

def overlay(original_path, heatmap, out_path, alpha=0.4):
    img = cv2.cvtColor(cv2.imread(original_path), cv2.COLOR_BGR2RGB)
    hm  = cv2.resize(heatmap, (img.shape[1], img.shape[0]))
    hm  = cv2.applyColorMap(np.uint8(255*hm), cv2.COLORMAP_JET)
    blended = np.uint8(hm * alpha + img)
    cv2.imwrite(out_path, cv2.cvtColor(blended, cv2.COLOR_RGB2BGR))

# ─────────────────────────── endpoints ────────────────────────────
@app.route('/gradcam', methods=['POST'])
def breast_gradcam():
    """POST an image, get prediction (+ heatmap if Cancer)."""
    if 'image' not in request.files:
        return jsonify({'error': 'No image part'}), 400

    # save upload
    file     = request.files['image']
    fname    = secure_filename(file.filename)
    in_path  = os.path.join(UPLOAD_FOLDER, fname)
    file.save(in_path)

    # predict
    img_arr  = preprocess(in_path)
    prob     = float(model.predict(img_arr)[0][0])   # sigmoid 0-1
    pred_cls = 0 if prob < 0.5 else 1                # 0=Cancer, 1=Non-Cancer

    if pred_cls == 1:  # Non-cancer → no Grad-CAM
        return jsonify({
            'prediction': 1,
            'confidence': prob,
            'message'   : 'Non-Cancer; no Grad-CAM generated.'
        })

    # Cancer → generate Grad-CAM
    heatmap   = make_heatmap(img_arr)
    out_path  = os.path.join(OUTPUT_FOLDER, 'gradcam_breast.jpg')
    overlay(in_path, heatmap, out_path)

    return jsonify({
        'prediction' : 0,
        'confidence' : prob,
        'message'    : 'Cancer; Grad-CAM generated.',
        'gradcam_url': '/gradcam-image'
    })

@app.route('/gradcam-image', methods=['GET'])
def send_cam():
    path = os.path.join(OUTPUT_FOLDER, 'gradcam_breast.jpg')
    if not os.path.exists(path):
        return jsonify({'error': 'Grad-CAM image not found'}), 404
    return send_file(path, mimetype='image/jpeg')

# ──────────────────────────── run app ─────────────────────────────
if __name__ == '__main__':
    # different port so it can run alongside the lung server
    app.run(host='0.0.0.0', port=5001, debug=True)
