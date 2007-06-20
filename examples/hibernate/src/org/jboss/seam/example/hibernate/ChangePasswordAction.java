//$Id$
package org.jboss.seam.example.hibernate;

import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.faces.FacesMessages;

@Name("changePassword")
public class ChangePasswordAction
{
   @In @Out
   private User user;
   
   @In
   private Session bookingDatabase;
   
   private String verify;
   
   public String changePassword()
   {
      if ( user.getPassword().equals(verify) )
      {
         user = (User) bookingDatabase.merge(user);
         return "main";
      }
      else 
      {
         FacesMessages.instance().add("Re-enter new password");
         bookingDatabase.refresh(user);
         verify=null;
         return null;
      }
   }
   public String cancel()
   {
      bookingDatabase.refresh(user);
      return "main";
   }
   public String getVerify()
   {
      return verify;
   }
   public void setVerify(String verify)
   {
      this.verify = verify;
   }
}
