package org.jboss.seam.transaction;

import static org.jboss.seam.annotations.Install.BUILT_IN;

import java.util.Stack;

import javax.transaction.Synchronization;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

/**
 * This implementation does not have access
 * to the JTA TransactionManager, so it is not fully aware
 * of container managed transaction lifecycle, and is not
 * able to register Synchronizations with a container managed 
 * transaction.
 * 
 * @author Gavin King
 * 
 */
@Name("org.jboss.seam.transaction.synchronizations")
@Scope(ScopeType.EVENT)
@Install(precedence=BUILT_IN)
@BypassInterceptors
public class SeSynchronizations implements Synchronizations
{
   protected Stack<SynchronizationRegistry> synchronizations = new Stack<SynchronizationRegistry>();
   
   public void afterBegin()
   {
      synchronizations.push( new SynchronizationRegistry() );
   }
   
   public void afterCommit(boolean success)
   {
      synchronizations.pop().afterTransactionCompletion(success);
   }
   
   public void afterRollback()
   {
      synchronizations.pop().afterTransactionCompletion(false);
   }
   
   public void beforeCommit()
   {
      synchronizations.peek().beforeTransactionCompletion();
   }
   
   public void registerSynchronization(Synchronization sync)
   {
      synchronizations.peek().registerSynchronization(sync);
   }
   
   public boolean isAwareOfContainerTransactions()
   {
      return false;
   }
   
}
