package eu.spaziodati.datatxt.stanbol.engine.enhancer.translators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.spaziodati.datatxt.stanbol.engine.enhancer.client.DatatxtResponse;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static eu.spaziodati.datatxt.stanbol.engine.enhancer.client.DatatxtResponse.Annotation;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

/**
 * {@link FiseTranslator} writes annotations using the FISE ontology.
 *
 * @author Gaetano Prestia  <prestia@netseven.it>
 * @author Giuliano Mega    <mega@spaziodati.eu>
 */
public class FiseTranslator implements ITranslator {

    private static final Logger LOG = LoggerFactory.getLogger(FiseTranslator.class);

    private static final LiteralFactory literalFactory = LiteralFactory.getInstance();

    private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final TranslationSupport fSupport;

    public FiseTranslator(TranslationSupport support) {
        fSupport = support;
    }

    public void translate(ContentItem ci, EnhancementEngine engine, DatatxtResponse datatxtResponse) {
        LOG.info(String.format("DatatxtAnnotator: Enhance ContentItem with FISE Annotations: ContentItem=%s, DatatxtResponse=%s", (ci != null ? ci.getUri() : ci), GSON.toJson(datatxtResponse)));
        if (ci != null && datatxtResponse != null) {
            if (datatxtResponse.annotations != null) {
                for (Annotation a : datatxtResponse.annotations) {
                    UriRef textAnnotation = createTextAnnotation(ci, engine, a, datatxtResponse.lang);
                    UriRef entityAnnotation = createEntityAnnotation(ci, engine, a, datatxtResponse.lang, textAnnotation);
                    fSupport.addEntity(ci, a, datatxtResponse.lang);
                }
            }
            addDetectedLanguage(ci, engine, datatxtResponse);
        }
    }

    /**
     * Creates a fise:TextAnnotation and adds it to the {@link ContentItem#getMetadata()}.
     *
     * @return the URI of the created fise:TextAnnotation
     */
    public static UriRef createTextAnnotation(ContentItem ci, EnhancementEngine engine, Annotation a, String lang) {
        MGraph g = ci.getMetadata();
        UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, engine);
        g.add(new TripleImpl(textAnnotation, ENHANCER_START, literalFactory.createTypedLiteral(a.start)));
        g.add(new TripleImpl(textAnnotation, ENHANCER_END, literalFactory.createTypedLiteral(a.end)));
        g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(a.spot)));
        return textAnnotation;
    }

    /**
     * Creates a fise:EntityAnnotation and adds it to the {@link ContentItem#getMetadata()}.
     *
     * @return the URI of the created fise:EntityAnnotation
     */
    public UriRef createEntityAnnotation(ContentItem ci, EnhancementEngine engine, Annotation a, String lang, UriRef textAnnotation) {
        MGraph g = ci.getMetadata();
        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, engine);
        // TODO: uri: gli extra_types non hanno uri! se ne crea una fittizia
        String _uri = a.uri != null ? a.uri : fSupport.getExtraTypesDummyUri(a.title);
        g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, new UriRef(_uri)));
        g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(a.title, new Language(lang))));
        g.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral((double) a.confidence)));
        if (a.types != null) {
            for (String type : a.types) {
                g.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, new UriRef(type)));
            }
        }
        // link ENTITY_ANNOTATION to TEXT_ANNOTATION
        g.add(new TripleImpl(entityAnnotation, DC_RELATION, textAnnotation));
        return entityAnnotation;
    }


    /**
     * Creates a fise:EntityAnnotation and adds it to the {@link ContentItem#getMetadata()}.
     *
     * @return the URI of the created fise:EntityAnnotation
     */
    public void addDetectedLanguage(ContentItem ci, EnhancementEngine engine, DatatxtResponse datatxtResponse) {
        String lang = EnhancementEngineHelper.getLanguage(ci);
        if (lang == null && ci != null && datatxtResponse != null && datatxtResponse.lang != null) {
            MGraph g = ci.getMetadata();
            UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, engine);
            g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(datatxtResponse.lang)));
            g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral((double) datatxtResponse.langConfidence)));
            g.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));
        }
    }


}