package eu.spaziodati.datatxt.stanbol.enhancer.engines.client;

import com.google.gson.GsonBuilder;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.DatatxtProperties;
import org.apache.commons.io.IOUtils;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gaetano Prestia  <prestia@netseven.it>
 * @author Giuliano Mega    <mega@spaziodati.eu>
 */
public class DatatxtClient implements DatatxtProperties {

    private static final Logger fLogger = LoggerFactory.getLogger(DatatxtClient.class);

    public static DatatxtClient newClient(Dictionary<String, Object> properties) throws ConfigurationException {
        PropertyHelper p = new PropertyHelper(properties);
        return new DatatxtClient(p.getString(DATATXT_NEX_URL),
                p.getString(DATATXT_APP_ID),
                p.getString(DATATXT_APP_KEY),
                p.getFloat(DATATXT_MIN_CONFIDENCE, DEFAULT_MIN_CONFIDENCE, 0f, 1f),
                p.getInt(DATATXT_MIN_LENGTH, DEFAULT_MIN_LENGTH, 0, Integer.MAX_VALUE),
                p.getString(DATATXT_EXTRA_TYPES, null),
                p.getString(DATATXT_COUNTRY, null),
                p.getString(DATATXT_CUSTOM_SPOTS, null)
        );
    }

    // ------------------------------------------------------------------------

    private URL fNexUrl;
    private String fAppId;
    private String fAppKey;
    private float fMinConfidence;
    private int fMinLength;
    private String fExtraTypes;
    private String fCountry;
    private String fCustomSpots;

    private DatatxtClient(String nexUrl, String appId, String appKey, float minConfidence, int minLength, String extraTypes,
                          String country, String customSpots) throws ConfigurationException {
        // check country
        if (extraTypes != null && !extraTypes.isEmpty()) {
            if (country != null) country = country.trim().toUpperCase();

            if (country != null && !country.isEmpty() && !ACCEPTED_COUNTRIES.contains(country)) {
                throw new ConfigurationException(DATATXT_COUNTRY, String.format("value=%s MUST BE IN %s", country, ACCEPTED_COUNTRIES));
            }
        }

        fNexUrl = url(nexUrl);

        fAppId = appId;
        fAppKey = appKey;
        fMinConfidence = minConfidence;
        fMinLength = minLength;
        fExtraTypes = extraTypes;
        fCountry = country;
        fCustomSpots = customSpots;
    }

    public DatatxtResponse doRequest(final String contentText, final String contentLang)
            throws DatatxtException {

        //TODO add permission check.
        final RequestHelper helper = new RequestHelper(contentText, contentLang);

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                helper.doRequest();
                return null;
            }
        });

        return helper.get();
    }

    private URL url(String url) throws ConfigurationException {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new ConfigurationException("Invalid URL", url, ex);
        }
    }

    private String query(String text, String lang) throws DatatxtException {
        try {
            return new QueryData()
                    .add("$app_id", fAppId).add("$app_key", fAppKey)
                    .add("lang", lang)
                    .add("min_confidence", String.valueOf(fMinConfidence))
                    .add("min_length", String.valueOf(fMinLength))
                    .add("extra_types", fExtraTypes).add("country", fCountry)
                    .add("custom_spots", fExtraTypes)
                    .add("include", "types,image,abstract")
                    .add("text", text)
                    .toString();
        } catch (UnsupportedEncodingException e) {
            throw new DatatxtException("Cannot prepare query data for the HttpRequest", e);
        }
    }
    /**
     * 
     * @param contentText
     * @param contentLang
     * @return
     * @throws UnmanagedLanguageException if the language of the parsed content is not
     * supported by DataTXT
     * @throws DatatxtException on any other error while calling the DataTXT service
     */
    private DatatxtResponse performRequest(String contentText, String contentLang) throws UnmanagedLanguageException, DatatxtException {

        fLogger.info(String.format("DatatxtAnnotator POSTing remote service: endpoint=%s", fNexUrl));

        // https://api.dandelion.eu/datatxt/nex/v1/?$app_id=APP_ID&$app_key=APP_KEY&include=types&text=THE_TEXT
        // Prepare query data
        String query = query(contentText, contentLang);

        if (fLogger.isDebugEnabled()) {
            fLogger.debug(String.format("*** nexUrl: %s", fNexUrl));
            fLogger.debug(String.format("*** query : %s", query));
        }

        // java HttpURLConnection, instead of third-part http library...
        HttpURLConnection connection = null;
        String responseContent = null;
        int responseCode;

        try {
            connection = (HttpURLConnection) fNexUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.writeBytes(query.toString());
                out.flush();
            }

            // Get response code
            responseCode = connection.getResponseCode();
            fLogger.info(String.format("*** responseCode: %d", responseCode));

            // Read response content
            InputStream is;
            if (responseCode / 100 != 2) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }
            responseContent = IOUtils.toString(is, StandardCharsets.UTF_8.name());
            is.close();
            if (responseCode / 100 != 2) {
                fLogger.warn(String.format("*** responseCode: %d, responseContent: %s", responseCode, responseContent));
            }
            if (fLogger.isDebugEnabled()) {
                fLogger.debug(String.format("*** responseContent: %s", responseContent));
            }
        } catch (Exception e) {
            throw new DatatxtException("Http Request could not be completed. Error: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        // Parse the response
        if (responseCode / 100 != 2) {
            // try detect {code: "error.unmanagedLanguage", message: "Unmanaged language [zz]"}
            DatatxtResponse response = new GsonBuilder().create().fromJson(responseContent, DatatxtResponse.class);
            if (response.error && response.code.contains(".unmanagedLanguage")) {
                Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(response.message);
                String unmanagedLanguage = matcher.find() ? matcher.group(1) : null;
                throw new UnmanagedLanguageException(unmanagedLanguage);
            }

            throw new DatatxtException("Invalid Response: Code=" + responseCode + ", Response=" + responseContent + ", Request=" + query, null);
        }

        try {
            DatatxtResponse response = new GsonBuilder().create().fromJson(responseContent, DatatxtResponse.class);
            response.text = contentText;
            return response;
        } catch (Exception e) {
            throw new DatatxtException("JSON Response could not be parsed. Error: " + e.getMessage(), e);
        }

    }

    private class RequestHelper {

        private volatile Exception fException;

        private volatile DatatxtResponse fResponse;

        private final String fText;

        private final String fLang;

        public RequestHelper(String text, String lang) {
            fLang = lang;
            fText = text;
        }

        public void doRequest() {
            try {
                fResponse = performRequest(fText, fLang);
            } catch (Exception ex) {
                fException = ex;
            }
        }

        public DatatxtResponse get() throws DatatxtException {
            if (fException != null) {
                raiseException();
            }

            return fResponse;
        }

        private void raiseException() throws DatatxtException {
            if (fException instanceof DatatxtException) {
                throw (DatatxtException) fException;
            }

            throw new DatatxtException("Error performing request.", fException);
        }

    }

    private static class PropertyHelper {

        private Dictionary<String, Object> properties;

        public PropertyHelper(Dictionary<String, Object> properties) {
            this.properties = properties;
        }

        public String getString(String name) throws ConfigurationException {
            return assertNonNull(name, getString(name, null));
        }

        public String getString(String name, String defaultValue) {
            Object value = properties.get(name);
            if (value == null) {
                return defaultValue;
            }

            String result;
            if (value instanceof String) {
                result = (String) value;
            } else if (value instanceof String[]) {
                // format as CSV
                result = Arrays.asList((String[]) value).toString().replaceAll(", ", ",").replaceAll("^\\[|\\]$", "");
            } else {
                // TODO: manage Object[] and iterable?
                result = value.toString();
            }

            return result;
        }

        public int getInt(String name, int defaultValue, int min, int max) throws ConfigurationException {
            Object value = properties.get(name);
            int result = defaultValue;
            if (value != null) {
                try {
                    result = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    fLogger.warn(String.format("Unable to parse Integer property '%s' from configured value '%s'! Use default '%d' instead.", name, value, defaultValue), e);
                }
            }
            if (result < min || result > max) {
                throw new ConfigurationException(name, String.format("Value %d falls outside of [%d, %d]", result, min, max));
            }
            return result;
        }

        public float getFloat(String name, float defaultValue, float min, float max) {
            Object value = properties.get(name);
            float result = defaultValue;
            if (value != null) {
                try {
                    result = Float.parseFloat(value.toString());
                } catch (NumberFormatException e) {
                    fLogger.warn(String.format("Unable to parse Float property '%s' from configured value '%s'! Use default '%d' instead.", name, value, defaultValue), e);
                }
            }
            if (result < min || result > max) {
                fLogger.warn(String.format("Configured '%s=%d' is invalid (value MUST BE IN [%d..%d]). Use default '%d' instead.", name, result, min, max, defaultValue));
                result = defaultValue;
            }
            return result;
        }

        private <T> T assertNonNull(String key, T value) throws ConfigurationException {
            if (value == null) {
                throw new ConfigurationException(key, "cannot be null");
            }
            return value;
        }

    }

}
