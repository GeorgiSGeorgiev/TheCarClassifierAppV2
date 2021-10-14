# README

## The Car Classifier - The Application

This code is part of my (Georgi S. Georgiev's) bachelor thesis "Neural network architectures for mobile devices" so with each usage it has to be cited. The thesis itself is in czech and I have written it as a student of the Charles University in Prague. Permanent link to the thesis can be found here: http://hdl.handle.net/20.500.11956/148274. If there are enough requests, I may try to translate the thesis to English. For more information see the English Abstract of the thesis which can be found in the link above as well.

### Short Introduction

The whole project contains two parts: an Android application written in Java and a backend program (the server program) written in Python. This folder contains only the application. The most important subfolders are described below.

This application contains the EfficientNetB0 model which can be evaluated directly on the chosen by the user images. The application can send the chosen images to the server as well and then to retrieve the result data. The communication with the server is realized via the HTTP protocol.

After starting the application it lets the user to choose an image of a car or to take a photo of it. After that the chosen image will be displayed on a special panel (View) and the user can evaluate it or send it directly to the server. The evaluation method is chosen in the settings of the application. The user has to set the right server IP address and the target port as well otherwise the communication with the server will fail.

If the main flask server is running and the image transfer is successful then the image will be downloaded and evaluated automatically on the other side. After that the result data and directly returned back to the application where the user can retrieve and see them. The application also saves the last result and allows the user to see it more than once (until the next evaluation is made or the application has been closed).

There is also a special grayscale evaluation mode. It makes the main CNN to evaluate both the RGB and the grayscale variant of the chosen image. Then the application chooses the more accurate result and returns it to the user.

### Project structure

1. Main modules (\app\src\main\java\com\example\thecarrecognizer)

   GoogleDriveController.java : Class that ensures the communication with Google Drive (image upload and data download).

   ImageBuilder.java : Image to Bitmap and Bitmap to Image converter.

   MainActivity.java : Contains the main initialization methods and connects all the other classes together.

   MainSettingsActivity : The settings Activity (panel) which contains the different settings of the application. Ensures correct creation of the settings panel.

   MLModel : Class which loads the CNN model from the application resources and enables its evaluation.

   ModelResultPair : Class representing one prediction of the CNN model. It contains a class label and a probability.

   OkHTTP3Controller : Class which defines the HTTP3 communication with the server part of the project.

   SettingsFragment : Class representing the settings menu back-end. Here is done the whole manipulation with the application cache.

   ThemeController : The main controller of the application themes.

2. Main components (\app\src\main\res)

   Contains definitions of all of the views, buttons, panels, dialogs, layouts, labels and even the main image

3. The EfficientNetB0 model (\app\src\main\ml)

   Contains the TensorFlow Lite version of the our best EfficientNetB0 model which was trained on the cars4 dataset.

4. Class Labels (\app\src\main\assets)

   Contains the names of the cars4 dataset labels.
