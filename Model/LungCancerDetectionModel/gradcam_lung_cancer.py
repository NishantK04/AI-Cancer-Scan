import tensorflow as tf
import numpy as np
import cv2
import matplotlib.pyplot as plt

# --- Load your model ---
model = tf.keras.models.load_model('cancer_model_mobilenet.keras')
print("Model loaded.")

# --- Get the MobileNetV2 backbone submodel ---
mobilenet_layer = model.get_layer('mobilenetv2_1.00_224')
print("MobileNetV2 internal layers:")
for layer in mobilenet_layer.layers:
    print(layer.name)

# After inspecting the above, pick the last conv layer.
# Usually for MobileNetV2, the last conv layer is 'block_16_project_BN'
last_conv_layer_name = 'block_16_project_BN'
print("Using last conv layer:", last_conv_layer_name)

# --- Preprocess input image ---
def load_and_preprocess_image(img_path, target_size=(224,224)):
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, target_size)
    img = img.astype(np.float32) / 255.0
    img = np.expand_dims(img, axis=0)  # add batch dim
    return img

# --- Grad-CAM function ---
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

# --- Overlay heatmap on image ---
def save_and_display_gradcam(img_path, heatmap, cam_path="cam.jpg", alpha=0.4):
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    heatmap = cv2.resize(heatmap, (img.shape[1], img.shape[0]))
    heatmap = np.uint8(255 * heatmap)

    heatmap_color = cv2.applyColorMap(heatmap, cv2.COLORMAP_JET)
    superimposed_img = heatmap_color * alpha + img
    superimposed_img = np.uint8(superimposed_img)

    cv2.imwrite(cam_path, cv2.cvtColor(superimposed_img, cv2.COLOR_RGB2BGR))

    plt.imshow(superimposed_img)
    plt.axis('off')
    plt.show()

# --- Main ---
if __name__ == "__main__":
    img_path = 'Malignant case (487).jpg'  # Your test image path here
    img_array = load_and_preprocess_image(img_path)

    preds = model.predict(img_array)
    print("Prediction probabilities:", preds)
    pred_class = np.argmax(preds[0])
    print(f"Predicted class: {pred_class} (0=benign, 1=malignant)")

    heatmap = make_gradcam_heatmap(img_array, mobilenet_layer, last_conv_layer_name, pred_class)
    save_and_display_gradcam(img_path, heatmap)
