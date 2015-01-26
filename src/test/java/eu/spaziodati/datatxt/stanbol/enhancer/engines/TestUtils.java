package eu.spaziodati.datatxt.stanbol.enhancer.engines;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.easymock.EasyMock;
import org.osgi.service.component.ComponentContext;

import eu.spaziodati.datatxt.stanbol.enhancer.engines.DatatxtNexEngine.OutputOntology;

import java.util.*;

public class TestUtils {

    public static String ENV_TEST_APP_ID = "APP_ID";

    public static String ENV_TEST_APP_KEY = "APP_KEY";

    public static String NEX_URL = "https://api.dandelion.eu/datatxt/nex/v1";

    public static String DATA_TXT_DEMO_TEXT = "The Mona Lisa is a 16th century oil painting created by " +
            "Leonardo. It's held at the Louvre in Paris.";

    public static Set<UriRef> ENTITIES = new HashSet<UriRef>() {{
        add(new UriRef("http://en.wikipedia.org/wiki/Mona_Lisa"));
        add(new UriRef("http://en.wikipedia.org/wiki/Oil_painting"));
        add(new UriRef("http://en.wikipedia.org/wiki/Leonardo_da_Vinci"));
        add(new UriRef("http://en.wikipedia.org/wiki/The_Louvre"));
        add(new UriRef("http://en.wikipedia.org/wiki/Paris"));
    }};

    public static ComponentContext mockComponentContext(OutputOntology outputOntology) throws Exception {
        ComponentContext context = EasyMock.createMock(ComponentContext.class);

        EasyMock.expect(context.getProperties())
                .andReturn(config(outputOntology))
                .anyTimes();

        EasyMock.replay(context);

        return context;
    }

    public static NamespacePrefixService mockPrefixService() throws Exception {
        NamespacePrefixService service = EasyMock.createMock(NamespacePrefixService.class);

        EasyMock.expect(service.getNamespace("foaf"))
                .andReturn("http://xmlns.com/foaf/0.1")
                .anyTimes();

        EasyMock.replay(service);

        return service;
    }

    public static Dictionary<String, Object> config() throws Exception {
        return config(null);
    }
    
    public static Dictionary<String, Object> config(OutputOntology outputOntology) throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();

        config.put(EnhancementEngine.PROPERTY_NAME, "datatxtAnnotate");
        config.put(DatatxtProperties.DATATXT_NEX_URL, NEX_URL);

        addProperty(config, ENV_TEST_APP_ID, DatatxtProperties.DATATXT_APP_ID);
        addProperty(config, ENV_TEST_APP_KEY, DatatxtProperties.DATATXT_APP_KEY);
        if(outputOntology == null){
            outputOntology = OutputOntology.FAM;
        }
        config.put(DatatxtNexEngine.PROPERTY_OUTPUT_ONTOLOGY, outputOntology.name());
        return config;
    }

    private static void addProperty(Dictionary<String, Object> config, String envKey, String engineKey) {
        String value = System.getenv(envKey);
        if (value == null) {
            throw new MissingResourceException("Missing " + envKey, TestUtils.class.getName(), envKey);
        }

        config.put(engineKey, value);
    }

}
