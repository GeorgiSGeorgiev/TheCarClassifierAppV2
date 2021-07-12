package com.example.thecarrecognizer;

import android.util.Pair;

import java.text.DecimalFormat;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Class representing one prediction. It contains a class label and a probability.
 */
public class ModelResultPair {
    private String label;
    private Float probability;

    /**
     * Basic constructor.
     * @param otherEntry Map entry which will be converted to a ModelResultPair.
     */
    public ModelResultPair(Map.Entry<String, Float> otherEntry) {
        super(); // call the object constructor first
        setTheWholePair(otherEntry); // set the pair
    }

    /**
     * Basic constructor.
     * @param otherEntry A pair which will be converted to a ModelResultPair.
     */
    public ModelResultPair(Pair<String, Float> otherEntry) {
        super();
        setTheWholePair(otherEntry);
    }

    /**
     * Basic constructor.
     * @param labelName The label name to be put in this new instance.
     * @param inProbability The probability which belongs to the label..
     */
    public ModelResultPair(String labelName, Float inProbability) {
        super();
        this.label = labelName;
        this.probability = inProbability;
    }

    /**
     * Standard hashing method.
     * @return The resulting hash code.
     */
    public int hashCode() {
        int hashLabel = label != null ? label.hashCode() : 0; // if !null then label.hash else 0
        int hashProb = probability != null ? probability.hashCode() : 0;
        return (hashLabel + hashProb) * hashProb + hashLabel;
    }

    /**
     * Standard equality comparison method.
     * @param other The other object we are comparing this one to.
     * @return True or false.
     */
    public boolean equals(Object other) {
        boolean compRes = false;
        if (other instanceof ModelResultPair) {
            ModelResultPair otherPair = (ModelResultPair) other;
            // standard label comparison
            compRes = (this.label.equals(otherPair.label));
            if (!compRes) return false;
            // standard probability comparison
            compRes = (this.probability.equals(otherPair.probability));
        }
        return compRes;
    }

    /**
     * Convert this instance to string. It has the following format:
     * "label, (probability*100)%", where the probability is rounded up to two decimal places.
     * @return The result string.
     */
    @Nonnull
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        return label + ", " + df.format(probability * 100) + "%";
    }

    /**
     * Get the label of this instance.
     * @return The class name.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label of this instance.
     * @param label The new label which will be set.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get the probability of this instance.
     * @return The probability value asa float.
     */
    public Float getProbability() {
        return probability;
    }

    /**
     * Set the probability of this instance.
     * @param probability The new probability which will be set as a float.
     */
    public void setProbability(Float probability) {
        this.probability = probability;
    }

    /**
     * Set the whole instance of this class to a new value.
     * @param otherPair The new pair which will update this instance.
     */
    public void setTheWholePair(Pair<String, Float> otherPair) {
        this.label = otherPair.first;
        this.probability = otherPair.second;
    }

    /**
     * Set the whole instance of this class to a new value.
     * @param otherEntry The new map entry which will update this instance.
     */
    public void setTheWholePair(Map.Entry<String, Float> otherEntry) {
        this.label = otherEntry.getKey();
        this.probability = otherEntry.getValue();
    }
}
