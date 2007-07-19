package org.jboss.seam.transaction;

import javax.ejb.Local;

/**
 * Local interface for EjbTransaction
 * 
 * @author Gavin King
 *
 */
@Local
public interface LocalEjbSychronizations extends Synchronizations
{
   public void destroy();
}
