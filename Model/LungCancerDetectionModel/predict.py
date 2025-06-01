import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image
import matplotlib.pyplot as plt

# Load the trained model
model = load_model("cancer_model_mobilenet.keras")

# Define class labels in the same order as in training
class_names = ['benign', 'malignant']

# Define the image path
img_path = "Malignant case (397).jpg"  # Make sure this path is correct

# Load and preprocess the image
img = image.load_img(img_path, target_size=(224, 224))
img_array = image.img_to_array(img)
img_array = np.expand_dims(img_array, axis=0) / 255.0  # Normalize

# Make prediction
prediction = model.predict(img_array)  # prediction shape: (1, 2)

# Get the predicted class index
predicted_index = np.argmax(prediction[0])
predicted_class = class_names[predicted_index]
confidence = prediction[0][predicted_index]

# Show result
print(f"Predicted class: {predicted_class} (Confidence: {confidence*100:.2f}%)")

# Optional: show the image
plt.imshow(img)
plt.title(f"Prediction: {predicted_class} ({confidence*100:.2f}%)")
plt.axis('off')
plt.show()
