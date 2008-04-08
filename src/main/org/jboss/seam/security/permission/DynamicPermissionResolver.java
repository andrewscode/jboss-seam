package org.jboss.seam.security.permission;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.Component;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.security.Identity;

/**
 * Resolves dynamically-assigned permissions kept in a persistent store, such as a 
 * database.
 * 
 * @author Shane Bryzak
 */
@Name("org.jboss.seam.security.dynamicPermissionResolver")
@Scope(APPLICATION)
@BypassInterceptors
@Install(precedence=FRAMEWORK)
@Startup
public class DynamicPermissionResolver implements PermissionResolver, Serializable
{   
   private static final String DEFAULT_PERMISSION_STORE_NAME = "accountPermissionStore";
   
   private AccountPermissionStore permissionStore;
   
   private static final LogProvider log = Logging.getLogProvider(DynamicPermissionResolver.class);   
   
   @Create
   public void create()
   {
      initPermissionStore();
   }
   
   protected void initPermissionStore()
   {
      if (permissionStore == null)
      {
         permissionStore = (AccountPermissionStore) Component.getInstance(DEFAULT_PERMISSION_STORE_NAME, true);
      }           
      
      if (permissionStore == null)
      {
         log.warn("no permission store available - please install a PermissionStore with the name '" +
               DEFAULT_PERMISSION_STORE_NAME + "' if dynamic permissions are required.");
      }
   }     
   
   public AccountPermissionStore getPermissionStore()
   {
      return permissionStore;
   }
   
   public void setPermissionStore(AccountPermissionStore permissionStore)
   {
      this.permissionStore = permissionStore;
   }
   
   public boolean hasPermission(Object target, String action)
   {      
      if (permissionStore == null) return false;
      
      Identity identity = Identity.instance();
      
      if (!identity.isLoggedIn()) return false;
      
      String targetName = Seam.getComponentName(target.getClass());
      if (targetName == null)
      {
         targetName = target.getClass().getName();
      }
      
      List<AccountPermission> permissions = permissionStore.listPermissions(targetName, action);
      
      String username = identity.getPrincipal().getName();
      
      for (AccountPermission permission : permissions)
      {
         if (username.equals(permission.getAccount()) && permission.getAccountType().equals(AccountType.user))
         {
            return true;
         }
         
         if (permission.getAccountType().equals(AccountType.role) && identity.hasRole(permission.getAccount()))
         {
            return true;
         }
      }      
      
      return false;
   }
}