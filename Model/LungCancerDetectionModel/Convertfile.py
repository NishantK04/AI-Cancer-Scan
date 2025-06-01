import tensorflow as tf

model = tf.keras.models.load_model('cancer_model_mobilenet.keras')
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('lung_model.tflite', 'wb') as f:
    f.write(tflite_model)
