package org.jboss.seam.ui.renderkit;


import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.jboss.seam.ui.component.UIFileUpload;
import org.jboss.seam.ui.util.HTML;
import org.jboss.seam.ui.util.cdk.RendererBase;
import org.jboss.seam.web.MultipartRequest;
import org.richfaces.cdk.annotations.JsfRenderer;

@JsfRenderer(type="org.jboss.seam.ui.FileUploadRenderer", family="org.jboss.seam.ui.FileUploadRenderer")
public class FileUploadRendererBase extends RendererBase
{

   @Override
   protected Class getComponentClass()
   {
      return UIFileUpload.class;
   }

   @Override
   protected void doEncodeEnd(ResponseWriter writer, FacesContext context, UIComponent component) throws IOException
   {
      UIFileUpload fileUpload = (UIFileUpload) component;

      writer.startElement(HTML.INPUT_ELEM, fileUpload);
      writer.writeAttribute(HTML.TYPE_ATTR, HTML.FILE_ATTR, null);

      String clientId = fileUpload.getClientId(context);
      writer.writeAttribute(HTML.ID_ATTR, clientId, null);
      writer.writeAttribute(HTML.NAME_ATTR, clientId, null);


      /*if (fileUpload.getAccept() != null)
      {
         writer.writeAttribute(HTML.ACCEPT_ATTR, fileUpload.getAccept(), "accept");
      }

      if (fileUpload.getStyleClass() != null)
      {
         writer.writeAttribute(HTML.CLASS_ATTR, fileUpload.getStyleClass(), JSF.STYLE_CLASS_ATTR);
      }

      if (fileUpload.getStyle() != null)
      {
         writer.writeAttribute(HTML.STYLE_ATTR, fileUpload.getStyle(),  "style");
      }*/

      HTML.renderHTMLAttributes(writer, component, HTML.INPUT_FILE_PASSTHROUGH_ATTRIBUTES_WITHOUT_DISABLED);


      writer.endElement(HTML.INPUT_ELEM);
   }

   @SuppressWarnings( "resource" )
   @Override
   protected void doDecode(FacesContext context, UIComponent component)
   {
      UIFileUpload fileUpload = (UIFileUpload) component;
      Object request = context.getExternalContext().getRequest();
      if(request instanceof HttpServletRequest)
      {
          HttpServletRequest httpRequest = (HttpServletRequest)request;
          try
          {
              if (httpRequest.getParts().size() > 0)
              {
                  // servlet 3.x
                  String clientId = component.getClientId( context );
                  Part part = httpRequest.getPart( clientId );
                  if (part != null)
                  {
                      fileUpload.setLocalInputStream( new WrappedFileUploadInputStream( part ) );
                      fileUpload.setLocalContentType( part.getContentType() );
                      fileUpload.setLocalFileName( part.getSubmittedFileName() );
                      fileUpload.setLocalFileSize( (int)part.getSize() );
                  }

                  return;
              }

          }
          catch ( IOException | ServletException e )
          {
              throw new RuntimeException( "Exception caught processing content parts of a multidata upload: " + e.getMessage(), e );
          }
      }

      ServletRequest servletRequest = (ServletRequest) context.getExternalContext().getRequest();

      if (!(servletRequest instanceof MultipartRequest))
      {
          servletRequest = unwrapMultipartRequest(servletRequest);
      }

      if (servletRequest instanceof MultipartRequest)
      {
         MultipartRequest multipartRequest = (MultipartRequest) servletRequest;

         String clientId = component.getClientId(context);
         fileUpload.setLocalInputStream(multipartRequest.getFileInputStream(clientId));
         fileUpload.setLocalContentType(multipartRequest.getFileContentType(clientId));
         fileUpload.setLocalFileName(multipartRequest.getFileName(clientId));
         fileUpload.setLocalFileSize(multipartRequest.getFileSize(clientId));
      }
   }


   /**
    * Delay calling getInputStream on a Part object until a read is required.
    *
    * Wildfly depends on the caller to call close after they call .getInputStream otherwise temp files
    * will be left around.   Seam passes around the InputStream everywhere which basically makes no
    * guarantee that close will be called.  Technically, we should set the Part on the UIFileUpload, but
    * this wrapper requires less changes.
    */
   class WrappedFileUploadInputStream extends InputStream
   {
       Part part;
       InputStream inputStream;

       public WrappedFileUploadInputStream( Part part )
       {
           this.part = part;
       }

       private InputStream getInputStream() throws IOException
       {
           if ( inputStream == null )
           {
               inputStream = part.getInputStream();
           }
           return inputStream;
       }

       @Override
       public int read() throws IOException
       {
           return getInputStream().read();
       }

       @Override
       public int available() throws IOException
       {
           return getInputStream().available();
       }

       @Override
       public void close() throws IOException
       {
           getInputStream().close();
       }

       @Override
       public synchronized void mark( int readlimit )
       {
           try
           {
               getInputStream().mark( readlimit );
           }
           catch ( IOException e )
           {
               throw new RuntimeException( e );
           }
       }

       @Override
       public synchronized void reset() throws IOException
       {
           getInputStream().reset();
       }

       @Override
       public boolean markSupported()
       {
           try
           {
               return getInputStream().markSupported();
           }
           catch ( IOException e )
           {
               throw new RuntimeException( e );
           }
       }

   }


   /**
    * Finds an instance of MultipartRequest wrapped within a request or its
    * (recursively) wrapped requests.
    */
   private static ServletRequest unwrapMultipartRequest(ServletRequest request)
   {
      while (!(request instanceof MultipartRequest))
      {
         boolean found = false;

         for (Method m : request.getClass().getMethods())
         {
            if (ServletRequest.class.isAssignableFrom(m.getReturnType())
                     && m.getParameterTypes().length == 0)
            {
               try
               {
                  request = (ServletRequest) m.invoke(request);
                  found = true;
                  break;
               }
               catch (Exception ex)
               { /* Ignore, try the next one */
               }
            }
         }

         if (!found)
         {
            for (Field f : request.getClass().getDeclaredFields())
            {
               if (ServletRequest.class.isAssignableFrom(f.getType()))
               {
                  try
                  {
                     request = (ServletRequest) f.get(request);
                  }
                  catch (Exception ex)
                  { /* Ignore */
                  }
               }
            }
         }

         if (!found) break;
      }

      return request;
   }

}
