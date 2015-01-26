package eu.spaziodati.datatxt.stanbol.enhancer.engines;

import eu.fusepool.p3.vocab.FAM;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.DatatxtNexEngine.OutputOntology;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.AnnotationConstants;

import org.junit.Assert;
import org.apache.clerezza.rdf.core.*;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateEntityAnnotation;
import static eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.AnnotationConstants.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DatatxtNexEngineTest {
    
    private static final Logger log = LoggerFactory.getLogger(DatatxtNexEngineTest.class);
    
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    private static final LiteralFactory lFactory = LiteralFactory.getInstance();

    private static final JenaSerializerProvider fSerializer = new JenaSerializerProvider();

    @Test
    public void testFAMEngine() throws Exception {
        DatatxtNexEngine engine = new DatatxtNexEngine();

        engine.bind(TestUtils.mockPrefixService());
        engine.activate(TestUtils.mockComponentContext(OutputOntology.FAM));

        ContentItem ci = ciFactory.createContentItem(new StringSource(TestUtils.DATA_TXT_DEMO_TEXT));
        engine.computeEnhancements(ci);
        
        debugEnhancementResults(ci);

        // Check entity annotations.
        MGraph graph = ci.getMetadata();
        Iterator<Triple> annotations = graph.filter(null, RDF_TYPE, OA_ANNOTATION);

        HashSet<UriRef> reference = new HashSet<>(TestUtils.ENTITIES);

        // Check language annotation (just the language).
        UriRef lang = (UriRef) get(graph.filter(null, RDF_TYPE, FAM.LanguageAnnotation), 1).getSubject();
        PlainLiteral language = (PlainLiteral) get(graph.filter(lang, DC_LANGUAGE, null), 1).getObject();
        Assert.assertEquals("en", language.getLexicalForm());

        int count = 0;
        while (annotations.hasNext()) {
            UriRef annotation = (UriRef) annotations.next().getSubject();
            checkEntityAnnotation(graph, annotation, reference, language.getLexicalForm());
            count++;
        }


        Assert.assertEquals(TestUtils.ENTITIES.size(), count);
    }
    
    @Test
    public void testFISEEngine() throws Exception {
        DatatxtNexEngine engine = new DatatxtNexEngine();

        engine.bind(TestUtils.mockPrefixService());
        engine.activate(TestUtils.mockComponentContext(OutputOntology.FISE));

        ContentItem ci = ciFactory.createContentItem(new StringSource(TestUtils.DATA_TXT_DEMO_TEXT));
        engine.computeEnhancements(ci);
        
        debugEnhancementResults(ci);

        
        MGraph metadata = ci.getMetadata();

        Map<UriRef,Resource> expected = new HashMap<UriRef,Resource>();
        expected.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expected.put(Properties.DC_CREATOR, lFactory.createTypedLiteral(DatatxtNexEngine.class.getName()));
        validateAllTextAnnotations(metadata, TestUtils.DATA_TXT_DEMO_TEXT, expected);
        
        Set<UriRef> expectedEntities = new HashSet<UriRef>(TestUtils.ENTITIES);
        
        Iterator<Triple> it = metadata.filter(null, RDF_TYPE, ENHANCER_ENTITYANNOTATION);
        while(it.hasNext()){
            Triple t = it.next();
            Assert.assertTrue(t.getSubject() instanceof UriRef);
            validateEntityAnnotation(metadata, (UriRef)t.getSubject(), expected);
            Iterator<Triple> refEntityIt = metadata.filter(t.getSubject(), ENHANCER_ENTITY_REFERENCE, null);
            Assert.assertTrue(refEntityIt.hasNext());
            Resource refEntity = refEntityIt.next().getObject();
            Assert.assertTrue(refEntity instanceof UriRef);
            Assert.assertFalse(refEntityIt.hasNext());
            Assert.assertTrue(expectedEntities.remove(refEntity));
        }
        Assert.assertTrue(expectedEntities.isEmpty());
        
    }
    
    
    /**
     * Logs the enhancement results as {@link SupportedFormat#TURTLE} on
     * DEBUG level.
     * @param ci the processed contentItem
     */
    private static void debugEnhancementResults(ContentItem ci) {
        if(log.isDebugEnabled()){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            fSerializer.serialize(out, ci.getMetadata(), SupportedFormat.TURTLE);
            log.debug("Enhancement Results:\n{}\n",new String(out.toByteArray(),UTF8));
        }
    }

    private void checkEntityAnnotation(MGraph graph, UriRef anno, Set<UriRef> reference, String language) {
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
        Triple label = get(graph.filter(body, FAM.entity_label, null), 1);

        // Label should be an RDF plain literal with the same language as the rest of the text.
        Assert.assertTrue(label.getObject() instanceof PlainLiteral);
        PlainLiteral literal = (PlainLiteral) label.getObject();
        Assert.assertEquals(literal.getLanguage().toString(), language);

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
