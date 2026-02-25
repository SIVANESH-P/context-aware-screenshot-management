package com.example.snapsenseai;

public class AnonymizedMetadataBuffer {
    private static AnonymizedMetadataBuffer instance;
    private String currentBuffer;

    private AnonymizedMetadataBuffer() {}

    public static synchronized AnonymizedMetadataBuffer getInstance() {
        if (instance == null) instance = new AnonymizedMetadataBuffer();
        return instance;
    }

    public void setBuffer(String text) { this.currentBuffer = text; }
    public String getBuffer() { return currentBuffer; }
}