/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.mock;

import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpSession;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Manager;
import org.jboss.seam.init.Initialization;
import org.jboss.seam.jsf.SeamNavigationHandler;
import org.jboss.seam.jsf.SeamPhaseListener;
import org.jboss.seam.jsf.SeamStateManager;
import org.jboss.seam.servlet.ServletSessionImpl;
import org.testng.annotations.Configuration;

/**
 * Superclass for TestNG integration tests for JSF/Seam applications.
 * 
 * @author Gavin King
 * @author <a href="mailto:theute@jboss.org">Thomas Heute</a>
 * @version $Revision$
 */
public class SeamTest
{

   private MockExternalContext externalContext;
   private MockServletContext servletContext;
   private MockLifecycle lifecycle;
   private MockApplication application;
   private SeamPhaseListener phases;
   private MockFacesContext facesContext;
   private Map<String, ConversationState> conversationStates;
   
   protected HttpSession getSession()
   {
      return (HttpSession) externalContext.getSession(true);
   }
   
   protected boolean isSessionInvalid()
   {
      return ( (MockHttpSession) getSession() ).isInvalid();
   }
   
   protected FacesContext getFacesContext()
   {
      return facesContext;
   }
   
   /**
    * Script is an abstract superclass for usually anonymous  
    * inner classes that test JSF interactions.
    * 
    * @author Gavin King
    */
   public abstract class Script
   {
      private String conversationId;
      private String outcome;
      private boolean isGetRequest;
      
      /**
       * A script for a JSF interaction with
       * no existing long-running conversation.
       */
      protected Script() {}
      
      /**
       * A script for a JSF interaction in the
       * scope of an existing long-running
       * conversation.
       */
      protected Script(String id)
      {
         conversationId = id;
      }
      
      protected Script(boolean isGetRequest)
      {
         this.isGetRequest = isGetRequest;
      }
      
      /**
       * Helper method for resolving components in
       * the test script.
       */
      protected Object getInstance(Class clazz)
      {
         return Component.getInstance(clazz, true);
      }

      /**
       * Helper method for resolving components in
       * the test script.
       */
      protected Object getInstance(String name)
      {
         return Component.getInstance(name, true);
      }
      
      /**
       * Override to implement the interactions between
       * the JSF page and your components that occurs
       * during the apply request values phase.
       */
      protected void applyRequestValues() throws Exception {}
      /**
       * Override to implement the interactions between
       * the JSF page and your components that occurs
       * during the process validations phase.
       */
      protected void processValidations() throws Exception {}
      /**
       * Override to implement the interactions between
       * the JSF page and your components that occurs
       * during the update model values phase.
       */
      protected void updateModelValues() throws Exception {}
      /**
       * Override to implement the interactions between
       * the JSF page and your components that occurs
       * during the invoke application phase.
       */
      protected void invokeApplication() throws Exception {}
      protected void setOutcome(String outcome) { this.outcome = outcome; }
      protected String getInvokeApplicationOutcome() { return outcome; }
      /**
       * Override to implement the interactions between
       * the JSF page and your components that occurs
       * during the render response phase.
       */
      protected void renderResponse() throws Exception {}
      /**
       * Override to set up any request parameters for
       * the request.
       */
      protected void setParameters() {}
      
      public Map getRequestParameterMap()
      {
         return externalContext.getRequestParameterMap();
      }
      
      public String run() throws Exception
      {   
         facesContext = new MockFacesContext( externalContext, application );
         facesContext.setCurrent();
         Map attributes = facesContext.getViewRoot().getAttributes();
         if (conversationId!=null) 
         {
            attributes.put(Manager.CONVERSATION_ID, conversationId);
         }
         else
         {
            attributes.remove(Manager.CONVERSATION_ID);
         }
         if ( conversationStates.containsKey( conversationId ) )
         {
            attributes.putAll(
                    conversationStates.get( conversationId ).state
            	);
         }
         
         setParameters();
         
         phases.beforePhase( new PhaseEvent(facesContext, PhaseId.RESTORE_VIEW, lifecycle ) );
         phases.afterPhase( new PhaseEvent(facesContext, PhaseId.RESTORE_VIEW, lifecycle ) );

         if (!isGetRequest)
         {
         
            phases.beforePhase( new PhaseEvent(facesContext, PhaseId.APPLY_REQUEST_VALUES, lifecycle ) );
            
            applyRequestValues();
      
            phases.afterPhase( new PhaseEvent(facesContext, PhaseId.APPLY_REQUEST_VALUES, lifecycle ) );
            phases.beforePhase( new PhaseEvent(facesContext, PhaseId.PROCESS_VALIDATIONS, lifecycle ) );
            
            processValidations();
      
            phases.afterPhase( new PhaseEvent(facesContext, PhaseId.PROCESS_VALIDATIONS, lifecycle ) );
            phases.beforePhase( new PhaseEvent(facesContext, PhaseId.UPDATE_MODEL_VALUES, lifecycle ) );
            
            updateModelValues();
      
            phases.afterPhase( new PhaseEvent(facesContext, PhaseId.UPDATE_MODEL_VALUES, lifecycle ) );
            phases.beforePhase( new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, lifecycle ) );
            
            invokeApplication();
            
            String outcome = getInvokeApplicationOutcome();
            facesContext.getApplication().getNavigationHandler().handleNavigation(facesContext, null, outcome);
      
            phases.afterPhase( new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, lifecycle ) );
            
            //TODO: this is the hack solution to redirects, would be much better to 
            //      actually really go ahead and emulate a redirect!
            boolean isRedirectScheduled = Manager.instance().isLongRunningConversation() && 
                  Manager.instance().getCurrentConversationEntry().isRemoveAfterRedirect();
            if ( isRedirectScheduled )
            {
               Manager.instance().setLongRunningConversation(false);
            }
         }
         
         phases.beforePhase( new PhaseEvent(facesContext, PhaseId.RENDER_RESPONSE, lifecycle ) );
         
         renderResponse();
         
         facesContext.getApplication().getStateManager().saveSerializedView(facesContext);
         
         phases.afterPhase( new PhaseEvent(facesContext, PhaseId.RENDER_RESPONSE, lifecycle ) );

         conversationId = (String) attributes.get(Manager.CONVERSATION_ID);         

         ConversationState conversationState = new ConversationState();
         conversationState.state.putAll( attributes );
         conversationStates.put( conversationId, conversationState );

         return conversationId;
      }
      
   }
   
   @Configuration(beforeTestMethod=true)
   public void begin()
   {
      externalContext = new MockExternalContext(servletContext);
   }

   @Configuration(afterTestMethod=true)
   public void end()
   {
      Lifecycle.endSession( servletContext, new ServletSessionImpl( (HttpSession) facesContext.getExternalContext().getSession(true) ) );
   }
   
   /**
    * Create a SeamPhaseListener by default. Override to use 
    * one of the other standard Seam phase listeners.
    */
   protected SeamPhaseListener createPhaseListener()
   {
	  return new SeamPhaseListener();
   }

   @Configuration(beforeTestClass=true)
   public void init() throws Exception
   {
      application = new MockApplication();
      application.setStateManager( new SeamStateManager( application.getStateManager() ) );
      application.setNavigationHandler( new SeamNavigationHandler( application.getNavigationHandler() ) );
      //don't need a SeamVariableResolver, because we don't test the view 
      phases = createPhaseListener();
      servletContext = new MockServletContext();
      
      initServletContext( servletContext.getInitParameters() );
      //Contexts.beginApplication(servletContext);
      lifecycle = new MockLifecycle();
      new Initialization(servletContext).init();
      Lifecycle.setServletContext(servletContext);

      conversationStates = new HashMap<String, ConversationState>();
   }

   @Configuration(afterTestClass=true)
   public void cleanup() throws Exception
   {
      Lifecycle.endApplication(servletContext);
      externalContext = null;
      conversationStates.clear();
      conversationStates = null;
   }
   
   /**
    * Override to set up any servlet context attributes.
    */
   public void initServletContext(Map initParams) {}

   private class ConversationState
   {
      Map state = new HashMap();
   }
}
