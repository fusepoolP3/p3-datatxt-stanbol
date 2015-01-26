/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.spaziodati.datatxt.stanbol.enhancer.engines;

import eu.spaziodati.datatxt.stanbol.enhancer.engines.client.DatatxtClient;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.client.DatatxtException;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.FamTranslator;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.FiseTranslator;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.ITranslator;
import eu.spaziodati.datatxt.stanbol.enhancer.engines.translators.TranslationSupport;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.*;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.enhancer.servicesapi.*;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

/**
 * {@link DatatxtNexEngine} is a Stanbol Enhancement Engine that uses the
 * <a href="https://dandelion.eu/docs/api/datatxt/nex/v1/">dataTXT Named Entity eXtraction API</a> to produce entity
 * annotations on text. <BR>
 * <BR>
 * Most of the configuration properties for this engine are defined in {@link DatatxtProperties}.
 *
 * @author Gaetano Prestia  <prestia@netseven.it>
 * @author Giuliano Mega    <mega@spaziodati.eu>
 *
 * @see DatatxtProperties
 */
@Component(immediate = true,
        metatype = true,
        inherit = true,
        label = "dataTXT-NEX: Named Entity eXtraction & Linking",
        description = "This engine performs Named Entity Extraction & Linking using dataTXT. It enables you to " +
                "automatically enrich your data by tagging texts with Wikipedia entities.")
@Reference(name = "prefix",
        referenceInterface = NamespacePrefixService.class)
@Service
@Properties(value = {
        @Property(name = EnhancementEngine.PROPERTY_NAME, value = "datatxtNex")
})
public class DatatxtNexEngine
        extends AbstractEnhancementEngine<IOException, RuntimeException>
        implements EnhancementEngine, ServiceProperties, DatatxtProperties {

    private static Logger fLogger = LoggerFactory.getLogger(DatatxtNexEngine.class);

    /**
     * The default order for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT} + 17. It should run after Metaxa and LangId.
     */
    public static final Integer DEFAULT_ORDER = ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT + 17;

    /**
     * The output ontology for the annotations produced by this engine.
     */
    public static enum OutputOntology {
        /**
         * Produces annotations using <a href="http://fise.iks-project.eu/ontology/">FISE</a>, the default ontology for
         * Stanbol 0.x.
         */
        FISE,
        /**
         * Produces annotations using the
         * <a href="https://github.com/fusepoolP3/overall-architecture/blob/master/wp3/fp-anno-model/fp-anno-model.md">
         * Fusepool P3 annotation model</a>.
         */
        FAM
    }

    /**
     * Property for controlling the output ontology for the annotations produced by this engine. Only the values
     * in {@link OutputOntology} are allowed, and are interpreted as {@link OutputOntology#valueOf(String)}.
     */
    @Property(options = {
            @PropertyOption(name = "FISE", value = DatatxtNexEngine.PROPERTY_OUTPUT_ONTOLOGY + ".option.fise"),
            @PropertyOption(name = "FAM", value = DatatxtNexEngine.PROPERTY_OUTPUT_ONTOLOGY + ".option.fam"),
    }, value = "FAM")
    public static final String PROPERTY_OUTPUT_ONTOLOGY = NAMESPACE + ".outputontology";

    private volatile NamespacePrefixService fPrefixService;

    private volatile ITranslator fTranslator;

    private volatile DatatxtClient fClient;

    public Map getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ServiceProperties.ENHANCEMENT_ENGINE_ORDERING,
                (Object) DEFAULT_ORDER));
    }

    @Override
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException, IOException {
        super.activate(ctx);
        Dictionary<String, Object> properties = ctx.getProperties();
        fTranslator = outputOntology(ctx, properties);
        fClient = DatatxtClient.newClient(properties);
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEnginee#canEnhance(org.apache.stanbol.enhancer.servicesapi.ContentItem)
     */
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        String text = text(ci);
        boolean canEnhance = text != null && !text.isEmpty();
        if (fLogger.isDebugEnabled()) {
            fLogger.debug("Request to enhance content with text " + text + " returns " + canEnhance);
        }

        return canEnhance ? ENHANCE_SYNCHRONOUS : CANNOT_ENHANCE;
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngine#computeEnhancements(org.apache.stanbol.enhancer.servicesapi.ContentItem)
     */
    public void computeEnhancements(ContentItem ci) throws EngineException {
        // We don't need to call ci.getLock().writeLock().lock()
        // as we're enhancing synchronously.
        String text = text(ci);
        try {
            fTranslator.translate(new ImmutablePair<UriRef, MGraph>(ci.getUri(), ci.getMetadata()),
                    this, text, fClient.doRequest(text, EnhancementEngineHelper.getLanguage(ci)));
        } catch (DatatxtException ex) {
            throw new EngineException(ex);
        }
    }

    private ITranslator outputOntology(ComponentContext context, Dictionary<String, Object> properties) throws ConfigurationException {
        String s = (String) properties.get(PROPERTY_OUTPUT_ONTOLOGY);
        OutputOntology outputOntology = s == null ? OutputOntology.FAM : OutputOntology.valueOf(s.toUpperCase());

        TranslationSupport support = new TranslationSupport(fPrefixService);
        switch (outputOntology) {
            case FISE:
                return new FiseTranslator(support);
            case FAM:
                return new FamTranslator(support);
            default:
                throw new IllegalStateException();
        }
    }

    private String text(ContentItem ci) throws InvalidContentException {
        try {
            String text = ContentItemHelper.getText(ci.getBlob());
            if (text != null) {
                return text.trim();
            }
        } catch (IOException e) {
            fLogger.error("Failed to get the text from content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        return null;
    }

    public void bind(NamespacePrefixService service) {
        fPrefixService = service;
    }

    public void unbind(NamespacePrefixService service) {
        fPrefixService = null;
    }

}
