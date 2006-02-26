package org.jboss.seam.example.remoting;

import static org.jboss.seam.ScopeType.SESSION;
import javax.ejb.Stateless;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import javax.ejb.Interceptors;
import org.jboss.seam.ejb.SeamInterceptor;

@Stateless
@Name("helloAction")
@Scope(SESSION)
@Interceptors(SeamInterceptor.class)
public class HelloAction implements HelloLocal {
  public String sayHello(String name) {
    return "Hello, " + name;
  }
}