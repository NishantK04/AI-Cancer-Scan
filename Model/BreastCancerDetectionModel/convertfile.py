import tensorflow as tf

model = tf.keras.models.load_model('breast_cancer_mobilenetv2.h5')
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

with open('breast_model.tflite', 'wb') as f:
    f.write(tflite_model)
