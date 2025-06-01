import tensorflow as tf
import numpy as np
import cv2
import matplotlib.pyplot as plt

# --- Load your trained breast cancer model ---
model = tf.keras.models.load_model('breast_cancer_mobilenetv2.h5')
print("Breast cancer model loaded.")

# --- Last convolutional layer name from MobileNetV2 ---
last_conv_layer_name = 'block_16_project_BN'

# --- Load and preprocess input image ---
def load_and_preprocess_image(img_path, target_size=(224,224)):
    img = cv2.imread(img_path)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = cv2.resize(img, target_size)
    img = img.astype(np.float32) / 255.0
    img = np.expand_dims(img, axis=0)
    return img

# --- Generate Grad-CAM heatmap ---
def make_gradcam_heatmap(img_array, model, last_conv_layer_name):
    grad_model = tf.keras.models.Model(
        inputs=model.inputs,
        outputs=[model.get_layer(last_conv_layer_name).output, model.output]
    )

    with tf.GradientTape() as tape:
        conv_outputs, predictions = grad_model(img_array)
        class_output = predictions[:, 0]  # Only one output neuron (sigmoid)

    grads = tape.gradient(class_output, conv_outputs)
    pooled_grads = tf.reduce_mean(grads, axis=(0, 1, 2))

    conv_outputs = conv_outputs[0]
    heatmap = conv_outputs @ pooled_grads[..., tf.newaxis]
    heatmap = tf.squeeze(heatmap)

    heatmap = tf.maximum(heatmap, 0) / tf.math.reduce_max(heatmap)
    return heatmap.numpy()

# --- Overlay heatmap on original image ---
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
    plt.title("Grad-CAM Visualization")
    plt.show()

# --- Main ---
if __name__ == "__main__":
    img_path = "IMG (600).jpg"  # Replace with real image path
    img_array = load_and_preprocess_image(img_path)

    preds = model.predict(img_array)
    pred_class = 0 if preds[0][0] < 0.5 else 1  # sigmoid threshold

    label_map = {0: "Cancer", 1: "Non-Cancer"}
    print(f"Prediction: {preds[0][0]:.4f} -> Class: {pred_class} ({label_map[pred_class]})")

    heatmap = make_gradcam_heatmap(img_array, model, last_conv_layer_name)
    save_and_display_gradcam(img_path, heatmap)
