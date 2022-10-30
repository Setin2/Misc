import os.path
import tensorflow as tf
from tensorflow import keras
from keras.preprocessing.image import ImageDataGenerator
from keras.models import Model
from keras.layers import Dense
from keras.models import Sequential

def create_model(resolution, load_previous_model=True):
  if os.path.isfile('catdog_model.h5') and load_previous_model: return tf.keras.models.load_model('catdog_model.h5')
  else:
    # here we need to implement the model
    model = tf.keras.models.Sequential([
        # since Conv2D is the first layer of the neural network, we should also specify the size of the input
        tf.keras.layers.Conv2D(16, (3,3), activation='relu', input_shape=(resolution, resolution, 3)),
        # apply pooling
        tf.keras.layers.MaxPooling2D(2,2),
        # and repeat the process
        tf.keras.layers.Conv2D(32, (3,3), activation='relu'),
        tf.keras.layers.MaxPooling2D(2,2), 
        tf.keras.layers.Conv2D(64, (3,3), activation='relu'), 
        tf.keras.layers.MaxPooling2D(2,2),
        # flatten the result to feed it to the dense layer
        tf.keras.layers.Flatten(), 
        # and define 512 neurons for processing the output coming by the previous layers
        tf.keras.layers.Dense(512, activation='relu'), 
        # a single output neuron. The result will be 0 if the image is a cat, 1 if it is a dog
        tf.keras.layers.Dense(1, activation='sigmoid')  
    ])
    model.compile(loss='binary_crossentropy', optimizer="adam", metrics=['accuracy'])
    return model

def train(training_set, validation_set, epochs, save):
  model.fit(training_set, steps_per_epoch = len(training_set), epochs=epochs, validation_data = validation_set, validation_steps = len(validation_set))
  model.save('catdog_model.h5')

"""
  repository is missing the actual files
  two directories are needed (train & validation) and each directory must have 2 subdirectories (cat, dog) with images of the repecting animal meow
"""
if __name__ == "__main__":
  resolution = 100 # 100x100x3
  data_generator = ImageDataGenerator(rescale=1./255, shear_range=0.2, zoom_range=0.2, rotation_range=45, horizontal_flip=True, vertical_flip=True, validation_split = .2, brightness_range=[0.4,1.5])
  training_set = data_generator.flow_from_directory('./train', target_size=(resolution, resolution), batch_size=32, class_mode='binary', subset='training')
  validation_set = data_generator.flow_from_directory('./validation', target_size=(resolution, resolution), batch_size=32, class_mode='binary', shuffle = False, subset='validation')

  model = create_model(resolution, load_previous_model=True)
  train(training_set, validation_set, epochs=5, save=True)
