package eu.spaziodati.datatxt.stanbol.engine.enhancer;

import eu.fusepool.p3.vocab.FAM;
import eu.spaziodati.datatxt.stanbol.engine.enhancer.translators.AnnotationConstants;
import junit.framework.Assert;
import org.apache.clerezza.rdf.core.*;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static eu.spaziodati.datatxt.stanbol.engine.enhancer.translators.AnnotationConstants.*;

import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DatatxtEnhancementEngineTest {

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    private static final LiteralFactory lFactory = LiteralFactory.getInstance();

    private static final JenaSerializerProvider fSerializer = new JenaSerializerProvider();

    @Test
    public void testFAMEngine() throws Exception {
        DatatxtNexEngine engine = new DatatxtNexEngine();

        engine.bind(TestUtils.mockPrefixService());
        engine.activate(TestUtils.mockComponentContext());

        ContentItem ci = ciFactory.createContentItem(new StringSource(TestUtils.DATA_TXT_DEMO_TEXT));
        engine.computeEnhancements(ci);

        // Check entity annotations.
        MGraph graph = ci.getMetadata();
        Iterator<Triple> annotations = graph.filter(null, RDF_TYPE, OA_ANNOTATION);

        HashSet<UriRef> reference = new HashSet<>(TestUtils.ENTITIES);

        int count = 0;
        while (annotations.hasNext()) {
            UriRef annotation = (UriRef) annotations.next().getSubject();
            checkEntityAnnotation(graph, annotation, reference);
            count++;
        }

        // Check language annotation (just the language).
        UriRef lang = (UriRef) get(graph.filter(null, RDF_TYPE, FAM.LanguageAnnotation), 1).getSubject();

        Assert.assertEquals(LiteralFactory.getInstance().createTypedLiteral("en"),
                get(graph.filter(lang, DC_LANGUAGE, null), 1).getObject());

        Assert.assertEquals(TestUtils.ENTITIES.size(), count);
    }

    private void checkEntityAnnotation(MGraph graph, UriRef anno, Set<UriRef> reference) {
        // Should have a body and a target.
        UriRef body = (UriRef) get(graph.filter(anno, AnnotationConstants.OA_HAS_BODY, null), 1).getObject();
        UriRef target = (UriRef) get(graph.filter(anno, AnnotationConstants.OA_HAS_TARGET, null), 1).getObject();

        // Both point to (the same) selector and source, but the FAM shortcuts uses a FAM predicates.
        UriRef bSelector = (UriRef) get(graph.filter(body, FAM.selector, null), 1).getObject();
        UriRef tSelector = (UriRef) get(graph.filter(target, AnnotationConstants.OA_HAS_SELECTOR, null), 1).getObject();
        Assert.assertEquals(bSelector.getUnicodeString(), tSelector.getUnicodeString());

        UriRef bTarget = (UriRef) get(graph.filter(body, FAM.extracted_from, null), 1).getObject();
        UriRef tTarget = (UriRef) get(graph.filter(target, AnnotationConstants.OA_HAS_SOURCE, null), 1).getObject();
        Assert.assertEquals(bTarget.getUnicodeString(), tTarget.getUnicodeString());

        // Bodies have LinkedEntity as type.
        typeOf(graph, target).contains(FAM.LinkedEntity);

        UriRef mention = (UriRef) get(graph.filter(body, FAM.entity_reference, null), 1).getObject();
        get(graph.filter(body, FAM.entity_label, null), 1);

        Assert.assertTrue(reference.remove(mention));
    }

    private Set<Resource> typeOf(MGraph graph, UriRef resource) {
        HashSet<Resource> types = new HashSet<Resource>();
        Iterator<Triple> typeTriples = graph.filter(resource, RDF_TYPE, null);

        while (typeTriples.hasNext()) {
            types.add(typeTriples.next().getObject());
        }

        return types;
    }

    Triple get(Iterator<Triple> it, int count) {
        Triple triple = null;
        for (int i = count; i > 0; i--) {
            Assert.assertTrue(it.hasNext());
            triple = it.next();
        }

        Assert.assertFalse(it.hasNext());
        return triple;
    }
}
