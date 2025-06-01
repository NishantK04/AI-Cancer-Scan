import numpy as np
import matplotlib.pyplot as plt
from tensorflow import keras
from tensorflow.keras.preprocessing import image

# 1. Load the trained model
model = keras.models.load_model('breast_cancer_mobilenetv2.h5')

# 2. Correct class labels
class_labels = {0: "Cancer", 1: "Non-Cancer"}  # 🔥 (Fixed!)

# 3. Load and preprocess the image
img_path = "IMG (600).jpg"  # <-- Replace this
img = image.load_img(img_path, target_size=(224, 224))
img_array = image.img_to_array(img)
img_array = np.expand_dims(img_array, axis=0)
img_array = img_array / 255.0

# 4. Predict
prediction = model.predict(img_array)

# 5. Interpret prediction
predicted_class_idx = 1 if prediction[0][0] > 0.5 else 0
predicted_class_label = class_labels[predicted_class_idx]
confidence = prediction[0][0] if prediction[0][0] > 0.5 else 1 - prediction[0][0]

# 6. Output
print(f"Prediction: {predicted_class_label}")
print(f"Confidence: {confidence * 100:.2f}%")

# 7. Show image
plt.imshow(img)
plt.title(f"{predicted_class_label}\nConfidence: {confidence * 100:.2f}%")
plt.axis('off')
plt.show()
