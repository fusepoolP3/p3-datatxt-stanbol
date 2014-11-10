package eu.spaziodati.datatxt.stanbol.enhancer.engines.translators;

import eu.fusepool.p3.vocab.FAM;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.DatatxtNexEngine;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.client.DatatxtResponse;
import org.apache.clerezza.rdf.core.*;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;

import java.util.Date;

import static eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.AnnotationConstants.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

/**
 * {@link FamTranslator} writes annotations using the
 * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md">
 * FAM</a> ontology.
 *
 * @author Giuliano Mega    <mega@spaziodati.eu>
 */
public class FamTranslator implements ITranslator {

    private static final int SELECTION_HEAD_TAIL = 200;

    private static final int SELECTION_PREFIX_SUFFIX = 10;

    private final LiteralFactory fLFactory = LiteralFactory.getInstance();

    private final TranslationSupport fSupport;

    public FamTranslator(TranslationSupport support) {
        fSupport = support;
    }

    @Override
    public void translate(Pair<UriRef, MGraph> item, EnhancementEngine engine, DatatxtResponse response) {
        MGraph graph = item.getValue();

        UriRef context = createContext(item);

        for (DatatxtResponse.Annotation rawAnnotation : response.annotations) {
            // Adds entities linked from this annotation.
            fSupport.addEntity(item, rawAnnotation, response.lang);

            // Creates FAM annotation.
            UriRef selector = selector(item, context, response.text, rawAnnotation);
            UriRef body = body(item, rawAnnotation, selector);
            UriRef target = target(item, body, selector);
            annotation(item, body, target);
        }

        addLanguage(item, response);
    }

    private UriRef body(Pair<UriRef, MGraph> item, DatatxtResponse.Annotation annotation, UriRef selector) {
        MGraph graph = item.getValue();

        UriRef body = new UriRef(mint("urn:enhancement-"));

        add(graph, body, RDF_TYPE, FAM.EntityAnnotation);

        add(graph, body, FAM.entity_reference, new UriRef(annotation.uri));
        add(graph, body, FAM.entity_label, literal(annotation.title));
        add(graph, body, FAM.confidence, literal(annotation.confidence));

        // FAM selector and source shortcuts.
        add(graph, body, FAM.selector, selector);
        add(graph, body, FAM.extracted_from, item.getKey());

        if (annotation.types != null) {
            for (String type : annotation.types) {
                add(graph, body, FAM.entity_type, new UriRef(type));
            }
        }

        return body;
    }

    private UriRef selector(Pair<UriRef, MGraph> item, UriRef contextUri, String text,
                            DatatxtResponse.Annotation annotation) {
        MGraph graph = item.getValue();
        UriRef selector = createRFC5147URI(item.getKey(), annotation.start, annotation.end);

        add(graph, selector, RDF_TYPE, NIF_STRING);
        add(graph, selector, RDF_TYPE, NIF_RFC5147STRING);

        add(graph, selector, NIF_BEGIN_INDEX, literal(annotation.start));
        add(graph, selector, NIF_END_INDEX, literal(annotation.end));
        add(graph, selector, NIF_ANCHOR_OF, literal(annotation.spot));

        add(graph, selector, NIF_HEAD, literal(head(text, 0, SELECTION_HEAD_TAIL)));
        add(graph, selector, NIF_TAIL, literal(tail(text, text.length() - 1, SELECTION_HEAD_TAIL)));

        add(graph, selector, NIF_BEFORE, literal(tail(text, annotation.start, SELECTION_PREFIX_SUFFIX)));
        add(graph, selector, NIF_AFTER, literal(head(text, annotation.end, SELECTION_PREFIX_SUFFIX)));

        add(graph, selector, NIF_REFERENCE_CONTEXT, contextUri);

        return selector;
    }

    private UriRef createContext(Pair<UriRef, MGraph> item) {
        MGraph graph = item.getValue();
        UriRef context = createRFC5147URI(item.getKey(), 0, null);

        add(graph, context, RDF_TYPE, NIF_CONTEXT);
        add(graph, context, RDF_TYPE, NIF_RFC5147STRING);
        add(graph, context, NIF_SOURCE_URL, item.getKey());

        return context;
    }

    private void addLanguage(Pair<UriRef, MGraph> item, DatatxtResponse response) {
        String lang = fSupport.getLanguage(item);
        // If there's already a language, leave it alone.
        if (lang != null) {
            return;
        }

        // dataTXT couldn't guess the language either. :-(
        if (response.lang == null) {
            return;
        }

        MGraph graph = item.getValue();
        UriRef languageAnno = new UriRef(mint("urn:enhancement-"));

        add(graph, languageAnno, RDF_TYPE, FAM.LanguageAnnotation);
        add(graph, languageAnno, DC_LANGUAGE, literal(response.lang));
        add(graph, languageAnno, FAM.confidence, literal(response.langConfidence));
        add(graph, languageAnno, FAM.extracted_from,  item.getKey());
    }

    private String head(String text, int offset, int length) {
        return text.substring(offset, Math.min(offset + length, text.length()));
    }

    private String tail(String text, int offset, int length) {
        return text.substring(Math.max(0, text.length() - SELECTION_HEAD_TAIL - offset), offset);
    }

    private UriRef target(Pair<UriRef, MGraph> item, UriRef body, UriRef selector) {
        MGraph graph = item.getValue();
        UriRef target = new UriRef(body.getUnicodeString() + SPTARGET_URI_SUFFIX);
        add(graph, target, RDF_TYPE, OA_SPECIFIC_RESOURCE);
        add(graph, target, OA_HAS_SELECTOR, selector);
        add(graph, target, OA_HAS_SOURCE, item.getKey());
        return target;
    }

    private UriRef annotation(Pair<UriRef, MGraph> item, UriRef body, UriRef target) {
        MGraph graph = item.getValue();

        UriRef enhancement = new UriRef(body.getUnicodeString() + ANNO_URI_SUFFIX);
        Date current = new Date();

        //XXX Do I have to set both ANNOTATED and SERIALIZED ?
        add(graph, enhancement, OA_SERIALIZED_AT, literal(current));
        add(graph, enhancement, OA_ANNOTATED_AT, literal(current));

        add(graph, enhancement, OA_SERIALIZED_BY, engineURI());
        add(graph, enhancement, OA_ANNOTATED_BY, engineURI());

        add(graph, enhancement, RDF_TYPE, OA_ANNOTATION);
        add(graph, enhancement, OA_HAS_BODY, body);
        add(graph, enhancement, OA_HAS_TARGET, target);

        return enhancement;
    }

    private String mint(String prefix) {
        return prefix + EnhancementEngineHelper.randomUUID();
    }

    /**
     * Creates an <a href="http://tools.ietf.org/html/rfc5147">RFC 5147</a>
     * compatible URI. In case start or end is <code>null</code> a URI selecting
     * the whole document will be returned.
     * <p/>
     * Taken from the <a href="https://github.com/fusepoolP3/p3-stanbol-engine-fam">
     * P3 Fise2Fam engine</a>.
     *
     * @param base  the base URI
     * @param start the start position or <code>null</code> if the whole text is selected
     * @param end   the end position or <code>null</code> ifthe whole text is selected
     * @return the RDC 5147 uri.
     */
    private UriRef createRFC5147URI(UriRef base, Integer start, Integer end) {
        StringBuilder sb = new StringBuilder(base.getUnicodeString());
        if (start == null || end == null) {
            sb.append("#char=0");
        } else {
            sb.append("#char=").append(start).append(',').append(end);
        }
        return new UriRef(sb.toString());
    }

    private Literal literal(Object value) {
        return fLFactory.createTypedLiteral(value);
    }

    private void add(MGraph graph, NonLiteral subject, UriRef predicate, Resource object) {
        graph.add(new TripleImpl(subject, predicate, object));
    }

    private Resource engineURI() {
        //TODO: use a real engine URI instead of the class name (from Rupert's TODO list ;-).
        return literal(DatatxtNexEngine.class.getName());
    }

}
