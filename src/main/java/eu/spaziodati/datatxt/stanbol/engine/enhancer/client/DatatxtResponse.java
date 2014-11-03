package eu.spaziodati.datatxt.stanbol.engine.enhancer.client;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;

public class DatatxtResponse {

    public int time;
    public String lang;
    public float langConfidence;
    public Collection<Annotation> annotations;

    // errors
    public boolean error;
    public String message;    // "Unmanaged language [zz]",
    public String code;        // "error.unmanagedLanguage",

    // include the request text for convenience.
    public transient String text;

    @Override
    public String toString() {
        return String.format("DatatxtResponse{%s, %f, %s}", lang, langConfidence, annotations);
    }

    public static class Annotation {

        public int start;
        public int end;
        public String spot;
        public float confidence;
        public String title;
        public String uri;

        @SerializedName("abstract")
        public String summary = null;
        public Collection<String> types;
        public Image image;

        @Override
        public String toString() {
            return String.format("Annotation{%s, %f, %s, %d, %d}", title, confidence, spot, start, end);
        }

    }

    public static class Image {

        public String full;
        public String thumbnail;

        @Override
        public String toString() {
            return String.format("Image{%s, %s}", full, thumbnail);
        }
    }
}
