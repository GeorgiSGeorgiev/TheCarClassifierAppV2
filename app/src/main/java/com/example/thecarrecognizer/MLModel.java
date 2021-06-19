package com.example.thecarrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

// import com.example.thecarrecognizer.ml.EfficientnetB0;
import com.example.thecarrecognizer.ml.Efficientnetb0V7;

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

public class MLModel {
    private final Context mainContext;
    private TensorImage processedImage;
    final String ASSOCIATED_AXIS_LABELS = "labels_cars.txt";
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
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(224, 224,
                                ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        // Create a TensorImage instance.
        // The data type is Float32 which matches the NN model input type.
        TensorImage tImage = new TensorImage(DataType.FLOAT32);

        // Load the bitmap and preprocess the image.
        tImage.load(photoBitmap);
        processedImage = imageProcessor.process(tImage);
    }

    private void loadLabels() {
        try {
            loadedLabels = FileUtil.loadLabels(mainContext, ASSOCIATED_AXIS_LABELS);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error: Reading the label file failed.", e);
        }
    }

    public List<String> getLoadedLabels() {
        return this.loadedLabels;
    }

    public ModelResultPair[] evalDirectly(Bitmap photoBitmap) {
        processImage(photoBitmap);
        loadLabels();
        ModelResultPair[] resultArray = new ModelResultPair[5];
        try {
            //Model model = Model.newInstance(mainContext);
            Efficientnetb0V7 model = Efficientnetb0V7.newInstance(mainContext);
            //Efficientnetb0V4 model = Efficientnetb0V4.newInstance(mainContext);
            // Creates inputs for reference.
            TensorBuffer inputTensor = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3},
                    DataType.FLOAT32);
            inputTensor.loadBuffer(processedImage.getBuffer());

            // Runs model inference and gets result.
            //Model.Outputs outputs = model.process(inputTensor);
            Efficientnetb0V7.Outputs outputs = model.process(inputTensor);
            TensorBuffer resultTensor = outputs.getOutputFeature0AsTensorBuffer();
            System.out.println(loadedLabels);

            if (loadedLabels != null) {
                // Map of labels and their corresponding probability
                TensorLabel labeledProbabilities = new TensorLabel(loadedLabels, resultTensor);

                // Create a map to access the result probabilities based on their labels.
                Map<String, Float> labeledResultMap = labeledProbabilities.getMapWithFloatValue();
                List<Map.Entry<String, Float>> list = new ArrayList<>(labeledResultMap.entrySet());
                list.sort(Map.Entry.comparingByValue());
                // Get the best 5 probabilities with their labels and save them in the result array.
                for (int i = 0; i < 5; i++) {
                    resultArray[i] = new ModelResultPair(list.get(list.size() - i - 1));
                    // System.out.println(resultArray[i]);
                }
            }
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            Log.e("tfliteModel",
                    "Error: The model loading failed. File could not be opened.", e);
        }
        return resultArray;
    }

    public String convertPairArrayToString(ModelResultPair[] resultPairs) {
        StringBuilder tmpStrBuilder = new StringBuilder();
        for (ModelResultPair resultPair : resultPairs) {
            tmpStrBuilder.append(resultPair);
            tmpStrBuilder.append('\n');
        }
        return tmpStrBuilder.toString();
    }
}
