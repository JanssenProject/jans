package org.xdi.oxauth.util;

import static org.testng.Assert.assertEquals;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.plugins.server.servlet.ServletSecurityContext;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.unbound.UnboundLiteral;
//import org.jboss.weld.environment.se.Weld;
//import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xdi.oxauth.servlet.OpenIdConfiguration;
import org.xdi.oxauth.session.ws.rs.EndSessionRestWebService;

/**
 * @author Gregor Tudan, Cofinpro AG
 */
public class SimpleRestEndpointTest {

//    private Logger log = LoggerFactory.getLogger(getClass());
//
//    private WeldContainer container;
//    private Dispatcher dispatcher;
//
//	private OpenIdConfiguration openIdConfiguration;
//
//	private EndSessionRestWebService endSessionRestWebService;
//    
//    @BeforeClass
//	public void initContainer() throws Exception {
//        Weld weld = new Weld();
//        this.container = weld.initialize();
//        RequestContext requestContext= container.instance().select(RequestContext.class, UnboundLiteral.INSTANCE).get();
//        requestContext.activate();
//        
//        ResteasyProviderFactory.getContextDataMap().put(SecurityContext.class, new FakeSecurityContext());
//        dispatcher = MockDispatcherFactory.createDispatcher();
//
//        endSessionRestWebService = container.instance().select(EndSessionRestWebService.class).get();
//        dispatcher.getRegistry().addSingletonResource(this.endSessionRestWebService);
//    }
//
//    @AfterClass
//	public void destroyContainer() throws Exception {
//    	this.container.shutdown();
//    }
//
//    @Test
//    public void testSendingHttpRequest() throws Exception {
//        MockHttpRequest request = MockHttpRequest.get("/oxauth/end_session").contentType(MediaType.TEXT_PLAIN_TYPE);
//        MockHttpResponse response = new MockHttpResponse();
//
//        dispatcher.invoke(request, response);
//        assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
//
//        assertEquals(response.getContentAsString(), "{\"error\":\"invalid_request\",\"error_description\":\"The request is missing a required parameter, includes an unsupported parameter or parameter value, repeats a parameter, or is otherwise malformed.\"}");
//    }
//
//    public static class FakeSecurityContext extends ServletSecurityContext {
//
//        public FakeSecurityContext() {
//            super(null);
//        }
//
//        @Override
//        public boolean isSecure() {
//           return true;
//        }
//
//        @Override
//        public String getAuthenticationScheme() {
//            return "unit-test-scheme";
//        }
//    }
// 
}
