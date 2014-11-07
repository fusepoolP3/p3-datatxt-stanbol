package eu.spaziodati.datatxt.stanbol.enhancer.engines.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class QueryData {

    StringBuilder query = new StringBuilder();

    public QueryData add(String key, String value) throws UnsupportedEncodingException {
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                if (query.length() != 0) query.append("&");
                query.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return query.toString();
    }

}

