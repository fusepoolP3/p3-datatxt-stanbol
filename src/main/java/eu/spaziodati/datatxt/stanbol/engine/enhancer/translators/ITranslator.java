package eu.spaziodati.datatxt.stanbol.engine.enhancer.translators;

import eu.spaziodati.datatxt.stanbol.engine.enhancer.client.DatatxtResponse;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * An {@link ITranslator} knows how to translate a {@link DatatxtResponse} into a series of RDF
 * statements using some translation strategy (typically bound to a specific ontology).
 *
 * @author Giuliano Mega    <mega@spaziodati.eu>
 */
public interface ITranslator {

    /**
     * Translates a {@link DatatxtResponse} into RDF {@link org.apache.clerezza.rdf.core.Triple}s,
     * storing them into {@link ContentItem#getMetadata()}.
     */
    void translate(ContentItem ci, EnhancementEngine engine, DatatxtResponse datatxtResponse);

}
