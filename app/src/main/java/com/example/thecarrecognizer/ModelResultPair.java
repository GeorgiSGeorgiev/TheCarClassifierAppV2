package com.example.thecarrecognizer;

import android.util.Pair;

import java.text.DecimalFormat;
import java.util.Map;

import javax.annotation.Nonnull;

public class ModelResultPair {
    private String label;
    private Float probability;

    public ModelResultPair(String label, Float probability) {
        super();
        this.label = label;
        this.probability = probability;
    }

    public ModelResultPair(Pair<String, Float> otherPair) {
        super();
        setTheWholePair(otherPair);
    }

    public ModelResultPair(Map.Entry<String, Float> otherEntry) {
        super();
        setTheWholePair(otherEntry);
    }

    public int hashCode() {
        // standard hashing method
        int hashLabel = label != null ? label.hashCode() : 0;
        int hashProb = probability != null ? probability.hashCode() : 0;
        return (hashLabel + hashProb) * hashProb + hashLabel;
    }

    public boolean equals(Object other) {
        boolean compRes = false;
        if (other instanceof ModelResultPair) {
            ModelResultPair otherPair = (ModelResultPair) other;
            // standard label comparison
            compRes = (this.label.equals(otherPair.label));
            if (!compRes) return compRes;
            // standard probability comparison
            compRes = (this.probability.equals(otherPair.probability));
        }
        return compRes;
    }

    @Nonnull
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.00");
        return label + ":  " + df.format(probability * 100) + "%";
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Float getProbability() {
        return probability;
    }

    public void setProbability(Float probability) {
        this.probability = probability;
    }

    public void setTheWholePair(Pair<String, Float> otherPair) {
        this.label = otherPair.first;
        this.probability = otherPair.second;
    }

    public void setTheWholePair(Map.Entry<String, Float> otherEntry) {
        this.label = otherEntry.getKey();
        this.probability = otherEntry.getValue();
    }
}
