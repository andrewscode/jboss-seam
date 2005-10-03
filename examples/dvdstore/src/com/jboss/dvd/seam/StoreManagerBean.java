/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */ 
package com.jboss.dvd.seam;

import java.io.Serializable;

import javax.ejb.Interceptor;
import javax.ejb.Stateless;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.ejb.SeamInterceptor;

@Stateless
@Name("stats")
@Interceptor(SeamInterceptor.class)
public class StoreManagerBean
    implements StoreManager,
               Serializable
{  
    @PersistenceContext(unitName="dvd")
    EntityManager em;

    public int getNumberOrders() {
        return (Integer) em.createQuery("select count(o) from Order o where o.status != :status")
            .setParameter("status", Order.Status.CANCELLED.ordinal())
            .getSingleResult();
    }

    public double getTotalSales() {
        try {
            return (Float) em.createQuery("select sum(o.totalAmount) from Order o where o.status != :status")
                .setParameter("status", Order.Status.CANCELLED.ordinal())
                .getSingleResult();
        } catch (EntityNotFoundException e) {
            return 0.0;
        }
    }

    public int getUnitsSold() {
        try {
            return (Integer) em.createQuery("select sum(i.sales) from Inventory i").getSingleResult();
        } catch (EntityNotFoundException e) {
            return 0;
        }
    }

    public int getTotalInventory() {
        try {
            return (Integer) em.createQuery("select sum(i.quantity) from Inventory i").getSingleResult();
        } catch (EntityNotFoundException e) {
            return 0;
        }
    }

}
