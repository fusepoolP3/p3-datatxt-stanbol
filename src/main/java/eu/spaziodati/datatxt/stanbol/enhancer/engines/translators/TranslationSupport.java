package eu.spaziodati.datatxt.stanbol.enhancer.engines.translators;

import eu.spaziodati.datatxt.stanbol.enhancer.engines.client.DatatxtResponse;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;

public class TranslationSupport {

    private static final Logger fLogger = LoggerFactory.getLogger(TranslationSupport.class);

    // TODO: make this configurable trough a property?
    public final UriRef RDFS_COMMENT = new UriRef(NamespaceEnum.rdfs + "comment");            // Short Abstracts

    public final UriRef FOAF_DEPICTION;

    public TranslationSupport(NamespacePrefixService service) {
        //FIXME should deal with the situation in which getNamespace returns null.
        FOAF_DEPICTION = new UriRef(service.getNamespace("foaf") + "depiction");
    }

    /**
     * Creates an Entity and adds it to the {@link org.apache.stanbol.enhancer.servicesapi.ContentItem#getMetadata()}.
     */
    public void addEntity(ContentItem ci, DatatxtResponse.Annotation a, String lang) {

        MGraph g = ci.getMetadata();

        // TODO: uri: gli extra_types non hanno uri! se ne crea una fittizia
        String _uri = a.uri != null ? a.uri : getExtraTypesDummyUri(a.title);
        UriRef entity = new UriRef(_uri);
        if (a.types != null) {
            for (String type : a.types) {
                g.add(new TripleImpl(entity, RDF_TYPE, new UriRef(type)));
            }
        }

        if (a.summary != null) {
            g.add(new TripleImpl(entity, RDFS_COMMENT, new PlainLiteralImpl(a.summary, new Language(lang))));
        }

        if (a.title != null) {
            g.add(new TripleImpl(entity, RDFS_LABEL, new PlainLiteralImpl(a.title, new Language(lang))));
        }

        // image
        if (a.image != null) {
            if (a.image.thumbnail != null) {
                g.add(new TripleImpl(entity, FOAF_DEPICTION, new UriRef(a.image.thumbnail)));
            } else if (a.image.full != null) {
                g.add(new TripleImpl(entity, FOAF_DEPICTION, new UriRef(a.image.full)));
            }
        }
    }

    public String getExtraTypesDummyUri(String title) {
        try {
            return "http://dandelion.eu/extra_types/" + URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            fLogger.error("Can't encode entity title " + title + ". Returning error URL", ex);
            return "http://dandelion.eu/extra_types/_bad_url";
        }
    }

}
