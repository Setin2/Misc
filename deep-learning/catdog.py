import os.path
import tensorflow as tf
import matplotlib.pyplot as plt
from tensorflow import keras
from keras.preprocessing.image import ImageDataGenerator

def create_model(resolution, load_previous_model=True):
  """ Return a keras model

  Either load a  preexisting model, if there is one, or create a new model from scratch
  """
  if os.path.isfile('catdog_model.h5') and load_previous_model: 
    return tf.keras.models.load_model('catdog_model.h5')
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
  history =  model.fit(training_set, steps_per_epoch = len(training_set), epochs=epochs, validation_data = validation_set, validation_steps = len(validation_set))
  model.save('catdog_model.h5')
  return history

def plot_learning_curve(history):
  history_dict = history.history
  loss_values = history_dict['loss']
  val_loss_values = history_dict['val_loss']
  accuracy = history_dict['accuracy']
  val_accuracy = history_dict['val_accuracy']

  epochs = range(1, len(loss_values) + 1)
  fig, ax = plt.subplots(1, 2, figsize=(14, 6))

  # plot accuracy
  ax[0].plot(epochs, accuracy, 'bo', label='Training accuracy')
  ax[0].plot(epochs, val_accuracy, 'b', label='Validation accuracy')
  ax[0].set_title('Training & Validation Accuracy')
  ax[0].set_xlabel('Epochs')
  ax[0].set_ylabel('Accuracy')
  ax[0].legend()

  # plot loss
  ax[1].plot(epochs, loss_values, 'bo', label='Training loss')
  ax[1].plot(epochs, val_loss_values, 'b', label='Validation loss')
  ax[1].set_title('Training & Validation Loss')
  ax[1].set_xlabel('Epochs')
  ax[1].set_ylabel('Loss')
  ax[1].legend()

  plt.savefig("accuracy&loss.png")
 
if __name__ == "__main__":
  resolution = 100 # 100x100x3
  data_generator = ImageDataGenerator(rescale=1./255, shear_range=0.2, zoom_range=0.2, rotation_range=45, horizontal_flip=True, vertical_flip=True, validation_split = .2, brightness_range=[0.4,1.5])
  training_set = data_generator.flow_from_directory('./train', target_size=(resolution, resolution), batch_size=32, class_mode='binary', subset='training')
  validation_set = data_generator.flow_from_directory('./validation', target_size=(resolution, resolution), batch_size=32, class_mode='binary', shuffle = False, subset='validation')

  model = create_model(resolution, load_previous_model=False)
  history = train(training_set, validation_set, epochs=100, save=True)
  plot_learning_curve(history)
