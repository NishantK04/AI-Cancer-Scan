from flask import Flask, request, jsonify, send_file
import tensorflow as tf
import numpy as np
import cv2
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

# Load the model once at server start
model = tf.keras.models.load_model('cancer_model_mobilenet.keras')
mobilenet_layer = model.get_layer('mobilenetv2_1.00_224')
last_conv_layer_name = 'block_16_project_BN'

UPLOAD_FOLDER = 'uploads'
GRADCAM_IMAGE_PATH = os.path.join(UPLOAD_FOLDER, 'gradcam.jpg')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def preprocess_image(image_path):
    img = cv2.imread(image_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, (224, 224))
    img = img.astype(np.float32) / 255.0
    img = np.expand_dims(img, axis=0)
    return img

def make_gradcam_heatmap(img_array, backbone_model, last_conv_layer_name, pred_index):
    grad_model = tf.keras.models.Model(
        inputs=backbone_model.inputs,
        outputs=[backbone_model.get_layer(last_conv_layer_name).output, backbone_model.output]
    )

    with tf.GradientTape() as tape:
        conv_outputs, predictions = grad_model(img_array)
        class_channel = predictions[:, pred_index]

    grads = tape.gradient(class_channel, conv_outputs)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))

    conv_outputs = conv_outputs[0]
    heatmap = conv_outputs @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)
    heatmap = tf.maximum(heatmap, 0) / tf.math.reduce_max(heatmap)
    return heatmap.numpy()

def overlay_heatmap(image_path, heatmap, cam_path="cam.jpg", alpha=0.4):
    img = cv2.imread(image_path)
    heatmap = cv2.resize(heatmap, (img.shape[1], img.shape[0]))
    heatmap = np.uint8(255 * heatmap)
    heatmap_color = cv2.applyColorMap(heatmap, cv2.COLORMAP_JET)
    superimposed_img = heatmap_color * alpha + img
    superimposed_img = np.uint8(superimposed_img)
    cv2.imwrite(cam_path, superimposed_img)

@app.route('/gradcam', methods=['POST'])
def handle_gradcam():
    if 'image' not in request.files:
        return jsonify({'error': 'No image provided'}), 400

    file = request.files['image']
    filename = secure_filename(file.filename)
    image_path = os.path.join(UPLOAD_FOLDER, filename)
    file.save(image_path)

    img_array = preprocess_image(image_path)
    preds = model.predict(img_array)
    pred_class = int(np.argmax(preds[0]))
    confidence = float(preds[0][pred_class])

    print(f"Predicted class: {pred_class} (0=benign, 1=malignant), confidence={confidence:.4f}")

    # If class is benign (0), skip Grad-CAM
    if pred_class == 0:
        return jsonify({'message': 'Benign case. No Grad-CAM generated.', 'prediction': 0, 'confidence': confidence})

    # Generate Grad-CAM
    heatmap = make_gradcam_heatmap(img_array, mobilenet_layer, last_conv_layer_name, pred_class)
    overlay_heatmap(image_path, heatmap, GRADCAM_IMAGE_PATH)

    return jsonify({
        'message': 'Grad-CAM generated',
        'prediction': 1,
        'confidence': confidence,
        'gradcam_url': '/gradcam-image'
    })

@app.route('/gradcam-image', methods=['GET'])
def get_gradcam_image():
    if os.path.exists(GRADCAM_IMAGE_PATH):
        return send_file(GRADCAM_IMAGE_PATH, mimetype='image/jpeg')
    else:
        return jsonify({'error': 'Grad-CAM image not found'}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
