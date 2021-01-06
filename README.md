# README

## The Car Classifier- The Application



### Short Introduction

The whole project contains two parts: an Android application written in Java and a backend program (the core) written in Python. This repository contains only the application. The most important subfolders are described below. You can directly follow the links to get to them.

The main purpose of this application is to send the chosen images to the core program and then to retrieve the result data. The communication with the core is realized via the Google Drive cloud services.

After starting the application it lets the user to choose an image of a car or to take a photo of it. After that the chosen image will be displayed on a special panel (View) and the user can send it directly to Drive. If the core program is running and the image transfer is successful then the image will be downloaded and evaluated automatically. After that the result data are saved to a text file and returned back to the application where the user can retrieve and see them. After the result is already seen it is automatically erased from the cloud alongside the uploaded image.



### Project structure

1. Main modules

   [GoogleDriveController.java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/blob/master/app/src/main/java/com/example/thecarrecognizer/GoogleDriveController.java) : Class that ensures the communication with Google Drive (image upload and data download)

   [ImageBuilder.java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/blob/master/app/src/main/java/com/example/thecarrecognizer/ImageBuilder.java) : Image to Bitmap and Bitmap to Image converter

   [MainActivity.java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/blob/master/app/src/main/java/com/example/thecarrecognizer/MainActivity.java) : Contains the main initialization methods and connects all the other classes together

   [ProgressDialog.java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/blob/master/app/src/main/java/com/example/thecarrecognizer/ProgressDialog.java) : Special progress dialog builder

   [ViewExtensions.java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/blob/master/app/src/main/java/com/example/thecarrecognizer/ViewExtensions.java) : Contains methods that "extend" the View class like button background changer

   * Containing folder location: [TheCarClassifier](https://github.com/GeorgiSGeorgiev/TheCarClassifier)/[app](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app)/[src](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src)/[main](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main)/[java](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main/java)/[com](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main/java/com)/[example](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main/java/com/example)/[thecarrecognizer](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main/java/com/example/thecarrecognizer)/

2. Main components

   * Description: Contains definitions of all of the views, buttons, panels, dialogs, layouts, labels

   * Containing folder location: [TheCarClassifier](https://github.com/GeorgiSGeorgiev/TheCarClassifier)/[app](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app)/[src](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src)/[main](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main)/[res](https://github.com/GeorgiSGeorgiev/TheCarClassifier/tree/master/app/src/main/res)/