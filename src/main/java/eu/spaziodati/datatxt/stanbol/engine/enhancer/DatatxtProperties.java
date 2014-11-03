package eu.spaziodati.datatxt.stanbol.engine.enhancer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;

import java.util.Arrays;
import java.util.Collection;

@Component(metatype = true, componentAbstract = true)
public interface DatatxtProperties {

    @Property(value = "https://api.dandelion.eu/datatxt/nex/v1", label = "Annotator endpoint URL",
            description = "The endpoint URL for Datatxt Annotator")
    public static final String DATATXT_NEX_URL = "eu.spaziodati.datatxt.stanbol.engine.enhancer.url";

    @Property(value = "", label = "Application ID",
            description = "The registerd Application ID (sign up for free at https://dandelion.eu/accounts/login)")
    public static final String DATATXT_APP_ID = "eu.spaziodati.datatxt.stanbol.engine.enhancer.app_id";

    @Property(value = "", label = "Application KEY",
            description = "The assigned Application KEY (sign up for free at https://dandelion.eu/accounts/login)")
    public static final String DATATXT_APP_KEY = "eu.spaziodati.datatxt.stanbol.engine.enhancer.app_key";

    @Property(value = "0.6", label = "Threshold for the Confidence value",
            description = "Entities with a confidence value below this threshold will be discarded."
                    + " Confidence is a numeric estimation of the quality of the annotation,"
                    + " which ranges between 0 and 1.")
    public static final String DATATXT_MIN_CONFIDENCE = "eu.spaziodati.datatxt.stanbol.engine.enhancer.min_confidence";
    public static final float DEFAULT_MIN_CONFIDENCE = 0.6f;

    @Property(value = "2", label = "Spot minimum length",
            description = "With this parameter you can remove those entities having a spot shorter than a minimum length.")
    public static final String DATATXT_MIN_LENGTH = "eu.spaziodati.datatxt.stanbol.engine.enhancer.min_length";
    public static final int DEFAULT_MIN_LENGTH = 2;

    @Property(value = {}, label = "Recognizes more type of entities", cardinality = Integer.MAX_VALUE,
            description = "Recognizes more type. Note: this parameter require the country parameter to be set, and VAT IDs will work only for Italy.", options = {
            @PropertyOption(name = "phone", value = "Phone numbers - Enables matching of phone numbers"),
            @PropertyOption(name = "vat", value = "VAT numbers - Enables matching of VAT IDs (Italian only)")})
    public static final String DATATXT_EXTRA_TYPES = "eu.spaziodati.datatxt.stanbol.engine.enhancer.extra_types";

    @Property(value = "", label = "Country for VAT and telephone extraction",
            description = "This parameter specifies the country which we assume VAT and telephone numbers to be coming from."
                    + " This is important to get correct results, as different countries may adopt different formats."
                    + " Accepted values: AD, AE, AM, AO, AQ, AR, AU, BB, BR, BS, BY, CA, CH, CL, CN, CX, DE, FR, GB, HU, IT, JP, KR, MX, NZ, PG, PL, RE, SE, SG, US, YT, ZW")
    public static final String DATATXT_COUNTRY = "eu.spaziodati.datatxt.stanbol.engine.enhancer.country";

    @Property(value = "", label = "Custom spots",
            description = "Enable specific collection of user-defined spots to be used when annotating the text."
                    + " You can define your own spots or use someone else's ones if they shared the spots-ID with you"
                    + " (to define your own spots: https://dandelion.eu/docs/api/datatxt/custom-spots/v1/)")
    public static final String DATATXT_CUSTOM_SPOTS = "eu.spaziodati.datatxt.stanbol.engine.enhancer.custom_spots";

    public static final Collection<String> ACCEPTED_COUNTRIES = Arrays.asList(new String[]{
            "AD", "AE", "AM", "AO", "AQ", "AR", "AU", "BB", "BR", "BS", "BY",
            "CA", "CH", "CL", "CN", "CX", "DE", "FR", "GB", "HU", "IT", "JP",
            "KR", "MX", "NZ", "PG", "PL", "RE", "SE", "SG", "US", "YT", "ZW"
    });

}
