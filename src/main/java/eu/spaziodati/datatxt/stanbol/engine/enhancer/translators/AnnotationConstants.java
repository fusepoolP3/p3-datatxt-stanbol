package eu.spaziodati.datatxt.stanbol.engine.enhancer.translators;

import org.apache.clerezza.rdf.core.UriRef;

/**
 * {@link AnnotationConstants} contains a set of constants with RDF IRIs (from
 * OpenAnnotation and NIF) and suffixes used for performing FAM annotations.
 * <p/>
 * Most of the content in this class has been copied from the <a href="https://github.com/fusepoolP3/p3-stanbol-engine-fam">
 * P3 Fise2Fam engine</a>.
 */
public class AnnotationConstants {

    public static final String ANNO_URI_SUFFIX = "-annotation";
    public static final String SPTARGET_URI_SUFFIX = "-sptarget";

    // OpenAnnotation
    public static final String NS_OA = "http://www.w3.org/ns/oa#";

    public static final UriRef OA_ANNOTATION = new UriRef(NS_OA + "Annotation");
    public static final UriRef OA_ANNOTATED_AT = new UriRef(NS_OA + "annotatedAt");
    public static final UriRef OA_ANNOTATED_BY = new UriRef(NS_OA + "annotatedBy");
    public static final UriRef OA_SERIALIZED_AT = new UriRef(NS_OA + "serializedAt");
    public static final UriRef OA_SERIALIZED_BY = new UriRef(NS_OA + "serializedBy");

    public static final UriRef OA_SPECIFIC_RESOURCE = new UriRef(NS_OA + "SpecificResource");
    public static final UriRef OA_HAS_BODY = new UriRef(NS_OA + "hasBody");
    public static final UriRef OA_HAS_TARGET = new UriRef(NS_OA + "hasTarget");
    public static final UriRef OA_HAS_SOURCE = new UriRef(NS_OA + "hasSource");
    public static final UriRef OA_HAS_SELECTOR = new UriRef(NS_OA + "hasSelector");

    // NIF
    public static final String NS_NIF = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
    public static final UriRef NIF_STRING = new UriRef(NS_NIF + "String");
    public static final UriRef NIF_RFC5147STRING = new UriRef(NS_NIF + "RDF5147String");
    public static final UriRef NIF_CONTEXT = new UriRef(NS_NIF + "Context");
    public static final UriRef NIF_SOURCE_URL = new UriRef(NS_NIF + "sourceURL");
    public static final UriRef NIF_BEGIN_INDEX = new UriRef(NS_NIF + "beginIndex");
    public static final UriRef NIF_END_INDEX = new UriRef(NS_NIF + "endIndex");
    public static final UriRef NIF_ANCHOR_OF = new UriRef(NS_NIF + "anchorOf");
    public static final UriRef NIF_HEAD = new UriRef(NS_NIF + "head");
    public static final UriRef NIF_TAIL = new UriRef(NS_NIF + "tail");
    public static final UriRef NIF_BEFORE = new UriRef(NS_NIF + "before");
    public static final UriRef NIF_AFTER = new UriRef(NS_NIF + "after");
    public static final UriRef NIF_REFERENCE_CONTEXT = new UriRef(NS_NIF + "referenceContext");

}
