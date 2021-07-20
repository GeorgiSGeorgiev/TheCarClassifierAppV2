package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

// Import the EfficientNetB0 model trained on our dataset.
import com.example.thecarrecognizer.ml.Efficientnetb024;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Created by: Georgi S. Georgiev
// Main information source: https://www.tensorflow.org/lite/guide/android
// The guide above contains the TF Lite Android SDK level requirements as well.
// The link was last accessed and checked on 20.07.2021.

/**
 * Class which represents the convolutional neural network model which can be evaluated from
 * the application. Contains mainly a link to the image, evaluation methods and
 * some supporting methods.
 */
public class MLModel {
    public static boolean grayscaleMode = true;
    private final Context mainContext;
    private TensorImage originalProcessedImage;
    private TensorImage grayscaleProcessedImage;

    final public static String ASSOCIATED_AXIS_LABELS = "labels_cars.txt";
    private List<String> loadedLabels = null;

    public MLModel(Context appContext) {
        mainContext = appContext;
    }

    /**
     * Processes the loaded bitmap and creates a tensor with the required dimensions.
     * The result bitmap tensor is stored internally in this class.
     * After completing this step you can directly call the model evaluation.
     *
     * @param photoBitmap The image bitmap to be processed.
     */
    private void processImage(Bitmap photoBitmap) {
        // imageProcessor resizes the image to match the input Tensor dimensions.
        // The preprocessor may cause image deformation which will result in biased evaluation.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(224, 224,
                                ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        // Create two TensorImage instances. Each of them will be preprocessed in a different way.
        // The data type is Float32 which matches the NN model input type.
        TensorImage origTensorImage = new TensorImage(DataType.FLOAT32);
        TensorImage grayTensorImage = new TensorImage(DataType.FLOAT32);

        // Load the original bitmap.
        origTensorImage.load(photoBitmap);
        // Load the grayscale bitmap.
        if (grayscaleMode) {
            Bitmap gray = ImageBuilder.fromRGBtoGrayscale(photoBitmap);
            grayTensorImage.load(gray);
        }
        // Use the both preprocessors independently on one another.
        originalProcessedImage = imageProcessor.process(origTensorImage);
        if (grayscaleMode) {
            grayscaleProcessedImage = imageProcessor.process(grayTensorImage);
        }
    }

    // Gets the labels from the Labels file in the spp project.
    private void loadLabels() {
        try {
            loadedLabels = FileUtil.loadLabels(mainContext, ASSOCIATED_AXIS_LABELS);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error: Reading the label file failed.", e);
        }
    }

    /**
     * Labels getter.
     * @return The loaded classification labels.
     */
    public List<String> getLoadedLabels() {
        return this.loadedLabels;
    }

    /**
     * Evaluate the loaded CNN model.
     * @param photoBitmap The image to be evaluated.
     * @return The evaluation results which are (label, probability) pairs.
     */
    public ModelResultPair[] evalDirectly(Bitmap photoBitmap) {
        processImage(photoBitmap);
        loadLabels();
        ModelResultPair[] resultArray = new ModelResultPair[5];
        try {
            // Create a new model instance. In the end of this method the instance will be
            // destroyed to maintain bigger memory efficiency.
            Efficientnetb024 model = Efficientnetb024.newInstance(mainContext);

            // Initializes the neural network inputs.
            TensorBuffer inputTensor = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3},
                    DataType.FLOAT32);
            // Load the already processed image.
            inputTensor.loadBuffer(originalProcessedImage.getBuffer());

            TensorBuffer grayInputTensor = null;
            if (grayscaleMode) {
                // If double evaluation is turned on, then initialize the grayscale inputs as well.
                grayInputTensor = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3},
                        DataType.FLOAT32);
                grayInputTensor.loadBuffer(grayscaleProcessedImage.getBuffer());
            }

            // Runs model inference and directly gets the results.
            Efficientnetb024.Outputs originalOutputs = model.process(inputTensor);
            Efficientnetb024.Outputs grayscaleOutputs;
            TensorBuffer resultGrayscaleTensor = null;
            if (grayscaleMode && grayInputTensor != null) {
                // If double evaluation is ON, then process the grayscale image as well.
                grayscaleOutputs = model.process(grayInputTensor);
                resultGrayscaleTensor = grayscaleOutputs.getOutputFeature0AsTensorBuffer();
            }

            // Convert the output features to a tensor buffer.
            TensorBuffer resultTensor = originalOutputs.getOutputFeature0AsTensorBuffer();

            if (loadedLabels != null) {
                // Map of labels and their corresponding probability
                TensorLabel labeledProbs = new TensorLabel(loadedLabels, resultTensor);
                // Create a map to access the result probabilities based on their labels.
                Map<String, Float> labeledResultMap = labeledProbs.getMapWithFloatValue();

                // Convert the map to a list of map entries. We need it to sort the the map entries
                // according to their values.
                List<Map.Entry<String, Float>> originalMapList =
                        new ArrayList<>(labeledResultMap.entrySet());
                // Sort the entries by their values.
                originalMapList.sort(Map.Entry.comparingByValue());

                List<Map.Entry<String, Float>> resultList;

                if (grayscaleMode && resultGrayscaleTensor != null) {
                    // The same as above just for grayscale images.
                    TensorLabel labeledGrayscaleProbs =
                            new TensorLabel(loadedLabels, resultGrayscaleTensor);
                    Map<String, Float> labeledGrayResultMap =
                            labeledGrayscaleProbs.getMapWithFloatValue();
                    List<Map.Entry<String, Float>> grayMapList =
                            new ArrayList<>(labeledGrayResultMap.entrySet());
                    grayMapList.sort(Map.Entry.comparingByValue());

                    // Get the top value gathered from the colored image.
                    float origListTopVal = originalMapList
                            .get(originalMapList.size() - 1).getValue();
                    // Get the top value gathered from the RGB image.
                    float grayListTopVal = grayMapList
                            .get(grayMapList.size() - 1).getValue();

                    // If the grayscale image evaluation is more accurate than the colored one
                    // (plus some offset), then consider the grayscale image result as the final
                    // one.
                    if (grayListTopVal > origListTopVal - 0.05) {
                        resultList = grayMapList;
                        System.out.println("Grayscale results chosen");
                    }
                    else {
                        resultList = originalMapList;
                        System.out.println("Original results chosen");
                    }
                } else { // The grayscale evaluation is turned off and we have the original result
                    resultList = originalMapList;
                    System.out.println("Original results chosen");
                }


                // Get the best 5 probabilities with their labels and save them
                // in the result array.
                for (int i = 0; i < 5; i++) {
                    resultArray[i] = new ModelResultPair(resultList.get(resultList.size() - i - 1));
                    System.out.println(resultArray[i]);
                }
            }
            // Releases model resources because they are no longer needed.
            model.close();
        } catch (IOException e) {
            Log.e("tfliteModel",
                    "Error: The model loading failed. File could not be opened.", e);
        }
        return resultArray;
    }

    /**
     * Convert the result pairs to a string.
     * @param resultPairs The result pairs to be converted.
     * @return The result pairs in the form of a string each on a separate line.
     */
    public String convertPairArrayToString(ModelResultPair[] resultPairs) {
        StringBuilder tmpStrBuilder = new StringBuilder();
        for (ModelResultPair resultPair : resultPairs) {
            tmpStrBuilder.append(resultPair);
            tmpStrBuilder.append('\n');
        }
        return tmpStrBuilder.toString();
    }
}
