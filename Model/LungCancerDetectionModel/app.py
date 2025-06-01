from flask import Flask, request, jsonify, send_file
import os
import tensorflow as tf
import numpy as np
import cv2
from werkzeug.utils import secure_filename

app = Flask(__name__)

UPLOAD_FOLDER = 'uploads'
OUTPUT_FOLDER = 'static'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

# Load your full model once
model = tf.keras.models.load_model('cancer_model_mobilenet.keras')
print("Model loaded.")

# Extract MobileNetV2 backbone (submodel inside your model)
mobilenet_layer = model.get_layer('mobilenetv2_1.00_224')
last_conv_layer_name = 'block_16_project_BN'  # Confirm this matches your model

def preprocess_image(img_path, target_size=(224,224)):
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, target_size)
    img = img.astype(np.float32) / 255.0
    img = np.expand_dims(img, axis=0)  # batch dimension
    return img

def make_gradcam_heatmap(img_array, backbone_model, last_conv_layer_name, pred_index=None):
    grad_model = tf.keras.models.Model(
        inputs=backbone_model.inputs,
        outputs=[backbone_model.get_layer(last_conv_layer_name).output, backbone_model.output]
    )
    with tf.GradientTape() as tape:
        conv_outputs, predictions = grad_model(img_array)
        if pred_index is None:
            pred_index = tf.argmax(predictions[0])
        class_channel = predictions[:, pred_index]
    grads = tape.gradient(class_channel, conv_outputs)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))
    conv_outputs = conv_outputs[0]
    heatmap = conv_outputs @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)
    heatmap = tf.maximum(heatmap, 0) / tf.math.reduce_max(heatmap)
    return heatmap.numpy()

def overlay_heatmap_on_image(original_img_path, heatmap, output_path, alpha=0.4):
    img = cv2.imread(original_img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    heatmap = cv2.resize(heatmap, (img.shape[1], img.shape[0]))
    heatmap = np.uint8(255 * heatmap)
    heatmap_color = cv2.applyColorMap(heatmap, cv2.COLORMAP_JET)
    superimposed_img = heatmap_color * alpha + img
    superimposed_img = np.uint8(superimposed_img)
    cv2.imwrite(output_path, cv2.cvtColor(superimposed_img, cv2.COLOR_RGB2BGR))

@app.route('/gradcam', methods=['POST'])
def gradcam_endpoint():
    if 'image' not in request.files:
        return jsonify({'error': 'No image uploaded'}), 400

    file = request.files['image']
    filename = secure_filename(file.filename)
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)
    print(f"Saved uploaded image to: {filepath}")

    img_array = preprocess_image(filepath)
    preds = model.predict(img_array)
    pred_class = int(np.argmax(preds[0]))
    confidence = float(preds[0][pred_class])
    print(f"Prediction: class {pred_class} with confidence {confidence:.4f}")

    if pred_class == 0:
        return jsonify({'message': 'Benign case. No Grad-CAM generated.', 'prediction': 0, 'confidence': confidence})

    heatmap = make_gradcam_heatmap(img_array, mobilenet_layer, last_conv_layer_name, pred_class)

    output_path = os.path.join(OUTPUT_FOLDER, 'gradcam.jpg')
    overlay_heatmap_on_image(filepath, heatmap, output_path)

    return jsonify({
        'message': 'Grad-CAM generated',
        'prediction': 1,
        'confidence': confidence,
        'gradcam_url': '/gradcam-image'
    })

@app.route('/gradcam-image', methods=['GET'])
def get_gradcam_image():
    output_path = os.path.join(OUTPUT_FOLDER, 'gradcam.jpg')
    if not os.path.exists(output_path):
        return jsonify({'error': 'Grad-CAM image not found'}), 404
    return send_file(output_path, mimetype='image/jpeg')

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
