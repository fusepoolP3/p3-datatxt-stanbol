package eu.spaziodati.datatxt.stanbol.engine.enhancer.client;

import eu.spaziodati.datatxt.stanbol.engine.enhancer.TestUtils;
import junit.framework.Assert;
import org.apache.clerezza.rdf.core.UriRef;
import org.junit.Test;

import java.net.URI;
import java.util.*;

public class DatatxtClientTest {

    @Test
    public void performsEnhancement() throws Exception {
        System.setSecurityManager(null);

        DatatxtClient client = DatatxtClient.newClient(TestUtils.config());
        DatatxtResponse response = client.doRequest("The Mona Lisa is a 16th century oil painting created by Leonardo. " +
                "It's held at the Louvre in Paris.", "en");

        assertURIContains(TestUtils.ENTITIES, response.annotations);
    }

    private void assertURIContains(Set<UriRef> entities, Collection<DatatxtResponse.Annotation> annotations) throws Exception {
        Set<UriRef> reference = new HashSet<UriRef>(TestUtils.ENTITIES);
        for (DatatxtResponse.Annotation annotation : annotations) {
            reference.remove(new UriRef(annotation.uri));
        }
        Assert.assertEquals(0, reference.size());
    }
}
