import os.path
import tensorflow as tf
import matplotlib.pyplot as plt
from tensorflow import keras
from keras.preprocessing.image import ImageDataGenerator
from keras.utils.vis_utils import plot_model
import numpy as np

def create_model(resolution, load_previous_model=True):
  """ Return a keras model

  Either load a  preexisting model, if there is one, or create a new model from scratch
  """
  if os.path.isfile('catdog_model.h5') and load_previous_model:
    return tf.keras.models.load_model('catdog_model.h5')
  else:
    # here we need to implement the model
    model = keras.models.Sequential([
      # since Conv2D is the first layer of the neural network, we should also specify the size of the input
      keras.layers.Conv2D(16, (3, 3), activation='relu', input_shape=(resolution, resolution, 3)),
      # apply pooling
      keras.layers.MaxPooling2D(2, 2),
      keras.layers.Dropout(0.2),
      # and repeat the process
      keras.layers.Conv2D(32, (3, 3), activation='relu'),
      keras.layers.MaxPooling2D(2, 2),
      keras.layers.Dropout(0.2),

      keras.layers.Conv2D(64, (3, 3), activation='relu'),
      keras.layers.MaxPooling2D(2, 2),
      keras.layers.Dropout(0.2),
      # flatten the result to feed it to the dense layer
      keras.layers.Flatten(),
      # and define 512 neurons for processing the output coming by the previous layers
      keras.layers.Dense(512, activation='relu'),
      # a single output neuron. The result will be 0 if the image is a cat, 1 if it is a dog
      keras.layers.Dense(1, activation='sigmoid')
    ])

    # compiling the model
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    # print the summary of the model
    print(model.summary())
    # save the model architecture into image file
    plot_model(model, show_shapes=True, to_file='model_plot.png')

    return model

def train(training_set, validation_set, epochs, save):
  early_stopping = keras.callbacks.EarlyStopping(monitor='val_loss', patience=20)
  history = model.fit(training_set, steps_per_epoch=len(training_set), epochs=epochs,
                      validation_data=validation_set, validation_steps=len(validation_set), callbacks=[early_stopping])
  model.save('catdog_model.h5')
  return history

def plot_learning_curve(history, figure_name):
  history_dict = history.history
  loss_values = history_dict['loss']
  val_loss_values = history_dict['val_loss']
  accuracy = history_dict['accuracy']
  val_accuracy = history_dict['val_accuracy']

  epochs = range(1, len(loss_values) + 1)
  fig, ax = plt.subplots(1, 2, figsize=(14, 6))

  # plot accuracy
  ax[0].plot(epochs, accuracy, label='Training accuracy')
  ax[0].plot(epochs, val_accuracy, label='Validation accuracy')
  ax[0].set_title('Training & Validation Accuracy')
  ax[0].set_xlabel('Epochs')
  ax[0].set_ylabel('Accuracy')
  ax[0].legend()

  # plot loss
  ax[1].plot(epochs, loss_values, label='Training loss')
  ax[1].plot(epochs, val_loss_values, label='Validation loss')
  ax[1].set_title('Training & Validation Loss')
  ax[1].set_xlabel('Epochs')
  ax[1].set_ylabel('Loss')
  ax[1].legend()

  plt.savefig(figure_name)

def model_eval(model, testing_set):
  # evaluate model
  results = model.evaluate(testing_set, batch_size=128)
  # print evaluation results
  print("test loss, test acc:", results)

  # create model predictions
  predictions = model.predict(testing_set)
  # normalize the predictions
  predictions[predictions <= 0.5] = 0
  predictions[predictions > 0.5] = 1

  # print predictions
  # print("Predictions: ", predictions)

def visualise(model, resolution):
  # ------------ Visualising filters ------------
  # get weights from first layer (conv2d)
  filters, biases = model.layers[0].get_weights()

  # normalize filter values (0-1) for visualization
  f_min, f_max = filters.min(), filters.max()
  filters = (filters - f_min) / (f_max - f_min)

  # visualize first 4 filters
  n_filters, ix = 4, 1
  for i in range(n_filters):
    # get the filter
    f = filters[:, :, :, i]
    # plot each channel separately
    for j in range(3):
      # specify subplot and turn of axis
      ax = plt.subplot(n_filters, 3, ix)
      ax.set_xticks([])
      ax.set_yticks([])
      # plot filter channel in grayscale
      plt.imshow(f[:, :, j], cmap='gray')
      ix += 1
  # show the figure
  plt.show()

  # ------------ Visualising the feature maps ------------

  # create new model for feature maps visualization (same as output of 1st layer of train model)
  feature_maps_model = tf.keras.Model(
    inputs=model.inputs, outputs=model.layers[0].output)

  # load image for visualization of feature maps
  image = keras.preprocessing.image.load_img(
    "./validation/cats/cat.1000.jpg", target_size=(resolution, resolution))

  # convert the image to array
  image = keras.preprocessing.image.img_to_array(image)
  # expand dimensions of the image
  image = np.expand_dims(image, axis=0)
  # caculate features map
  features = feature_maps_model.predict(image)

  # visualize multiple feature maps
  fig = plt.figure(figsize=(20, 15))
  for i in range(1, features.shape[3]+1):
    plt.subplot(8, 8, i)
    plt.xticks([])
    plt.yticks([])
    plt.imshow(features[0, :, :, i-1], cmap='gray')
  plt.show()

if __name__ == "__main__":
  resolution = 100  # 100x100x3
    data_generator = ImageDataGenerator(rescale=1./255, shear_range=0.2, zoom_range=0.2, rotation_range=45,
                                        horizontal_flip=True, vertical_flip=True, validation_split=.2, brightness_range=[0.4, 1.5])

    training_set = data_generator.flow_from_directory('./train', target_size=(
        resolution, resolution), batch_size=32, class_mode='binary', subset='training')
    validation_set = data_generator.flow_from_directory('./validation', target_size=(
        resolution, resolution), batch_size=32, class_mode='binary', shuffle=False, subset='validation')

    test_datagen = ImageDataGenerator(rescale=1./255)

    testing_set = test_datagen.flow_from_directory(
        './test', target_size=(resolution, resolution), class_mode='binary', shuffle=False)

    model = create_model(resolution, load_previous_model=False)
    history = train(training_set, validation_set, epochs=200, save=True)
    model_eval(model, testing_set)
    visualise(model, resolution)
    plot_learning_curve(history)
