package org.jboss.seam.ui.graphicImage;

import java.io.IOException;

import javax.faces.component.UIComponentBase;
import javax.faces.el.ValueBinding;

import org.jboss.seam.core.Image;
import org.jboss.seam.ui.JSF;

public class UIBlurImage extends UIComponentBase implements ImageTransform
{
   @Override
   public String getFamily()
   {
      return FAMILY;
   }
   
   private String radius;
   
   public void applyTransform(Image image, UIGraphicImage cmp) throws IOException
   {
      image.blur(new Integer(getRadius()));
   }
   
   public String getRadius()
   {
      if (radius != null)
      {
         return radius;
      }
      else
      {
         ValueBinding vb = getValueBinding("width");
         return vb == null ? null : JSF.getStringValue(getFacesContext(), vb);
      }
   }
   
   public void setRadius(String width)
   {
      this.radius = width;
   }
   
}
