/**
 * Copyright (c) 2019 Impact Mobile Inc.
 */
package org.jboss.seam.example.booking;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

@Name( "emTest" )
@Stateful
public class EmTest
{
    @In
    private EntityManager entityManager;

    public void test()
    {
        try
        {
            Query query = entityManager.createNativeQuery( "select * from hotel" );
            query.getResultList();

            Session session = entityManager.unwrap( Session.class );

            query = session.createNativeQuery( "select * from hotel" );

            query.getResultList();
            System.out.println( "sent query" );
        }
        catch ( Exception e )
        {
            System.out.println( "ERROR: " + e.getMessage() );
        }

    }

    @Remove
    public void destroy()
    {
    }
}
