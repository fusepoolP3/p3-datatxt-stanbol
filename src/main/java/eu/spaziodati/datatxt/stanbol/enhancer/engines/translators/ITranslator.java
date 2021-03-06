package eu.spaziodati.datatxt.stanbol.enhancer.engines.translators;

import eu.spaziodati.datatxt.stanbol.enhancer.engines.client.DatatxtResponse;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.lang3.tuple.Pair;
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
    void translate(Pair<UriRef, MGraph> item, EnhancementEngine engine, String text, DatatxtResponse datatxtResponse);

}
