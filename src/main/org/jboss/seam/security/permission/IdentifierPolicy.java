package org.jboss.seam.security.permission;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.security.permission.Identifier;

@Name("org.jboss.seam.security.identifierPolicy")
@Scope(APPLICATION)
@BypassInterceptors
@Install(precedence = Install.BUILT_IN)
public class IdentifierPolicy
{
   private Map<Class,IdentifierStrategy> strategies = new ConcurrentHashMap<Class,IdentifierStrategy>();
   
   private Set<IdentifierStrategy> registeredStrategies = new HashSet<IdentifierStrategy>();
   
   @Create
   public void create()
   {
      if (registeredStrategies.isEmpty())
      {
         registeredStrategies.add(new EntityIdentifierStrategy());
         registeredStrategies.add(new ClassIdentifierStrategy());
      }
   }
   
   public String getIdentifier(Object target)
   {
      IdentifierStrategy strategy = strategies.get(target.getClass());
      
      if (strategy == null)
      {
         if (target.getClass().isAnnotationPresent(Identifier.class))
         {
            Class<? extends IdentifierStrategy> strategyClass = 
               target.getClass().getAnnotation(Identifier.class).value();
            try
            {
               strategy = strategyClass.newInstance();
               strategies.put(target.getClass(), strategy);
            }
            catch (Exception ex)
            {
               throw new RuntimeException("Error instantiating IdentifierStrategy for object " + target, ex);
            }
         }
         else
         {
            for (IdentifierStrategy s : registeredStrategies)
            {
               if (s.canIdentify(target.getClass()))
               {
                  strategy = s;
                  strategies.put(target.getClass(), strategy);
                  break;
               }
            }
         }
      }
      
      return strategy.getIdentifier(target);
   }
   
   public Set<IdentifierStrategy> getRegisteredStrategies()
   {
      return registeredStrategies;
   }
   
   public void setRegisteredStrategies(Set<IdentifierStrategy> registeredStrategies)
   {
      this.registeredStrategies = registeredStrategies;
   }
}