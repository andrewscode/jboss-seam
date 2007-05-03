package org.jboss.seam.wiki.core.dao;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.wiki.core.model.Node;
import org.jboss.seam.wiki.core.model.Directory;
import org.jboss.seam.wiki.core.model.Document;
import org.jboss.seam.wiki.core.model.File;
import org.jboss.seam.Component;
import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.ScrollableResults;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.criterion.*;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * DAO for nodes, transparently respects security access levels.
 * <p>
 * All node access should go through this component, this component knows
 * about access levels because it relies on a restricted (filtered) Entitymanager.
 *
 * @author Christian Bauer
 *
 */
@Name("nodeDAO")
@AutoCreate
@Transactional
public class NodeDAO {

    // Most of the DAO methods use this
    @In protected EntityManager restrictedEntityManager;

    // Some run unrestricted (e.g. internal unique key validation of wiki names)
    // Make sure that these methods do not return detached objects!
    @In protected EntityManager entityManager;

    public void flushRegularEntityManager() {
        restrictedEntityManager.flush();
    }

    public void makePersistent(Node node) {
        entityManager.joinTransaction();
        entityManager.persist(node);
    }

    public Node findNode(Long nodeId) {
        restrictedEntityManager.joinTransaction();
        try {
            return (Node) restrictedEntityManager
                    .createQuery("select n from Node n where n.id = :nodeId")
                    .setParameter("nodeId", nodeId)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Node findNodeInArea(Long areaNumber, String wikiname) {
        return findNodeInArea(areaNumber, wikiname, restrictedEntityManager);
    }

    private Node findNodeInArea(Long areaNumber, String wikiname, EntityManager em) {
        em.joinTransaction();

        try {
            return (Node) em
                    .createQuery("select n from Node n where n.areaNumber = :areaNumber and n.wikiname = :wikiname")
                    .setParameter("areaNumber", areaNumber)
                    .setParameter("wikiname", wikiname)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Document findDocumentInArea(Long areaNumber, String wikiname) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Document) restrictedEntityManager
                    .createQuery("select d from Document d where d.areaNumber = :areaNumber and d.wikiname = :wikiname")
                    .setParameter("areaNumber", areaNumber)
                    .setParameter("wikiname", wikiname)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Directory findDirectoryInArea(Long areaNumber, String wikiname) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Directory) restrictedEntityManager
                    .createQuery("select d from Directory d where d.areaNumber = :areaNumber and d.wikiname = :wikiname")
                    .setParameter("areaNumber", areaNumber)
                    .setParameter("wikiname", wikiname)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Directory findArea(String wikiname) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Directory) restrictedEntityManager
                    .createQuery("select d from Directory d where d.parent = :root and d.wikiname = :wikiname")
                    .setParameter("root", Component.getInstance("wikiRoot"))
                    .setParameter("wikiname", wikiname)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Directory findArea(Long areaNumber) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Directory) restrictedEntityManager
                    .createQuery("select d from Directory d where d.parent = :root and d.areaNumber = :areaNumber")
                    .setParameter("root", Component.getInstance("wikiRoot"))
                    .setParameter("areaNumber", areaNumber)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public List<Document> findDocumentsInDirectoryOrderByCreatedOn(Directory directory, int firstResult, int maxResults) {
        //noinspection unchecked
        return (List<Document>)restrictedEntityManager
                .createQuery("select d from Document d where d.parent = :parentDir order by d.createdOn desc")
                .setParameter("parentDir", directory)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    public List<Document> findDocumentsOrderByLastModified(int maxResults) {
        //noinspection unchecked
        return (List<Document>)restrictedEntityManager
                .createQuery("select d from Document d order by d.lastModifiedOn desc")
                .setMaxResults(maxResults)
                .getResultList();
    }

    public Node findHistoricalNode(Long historyId) {
        Node historicalNode = (Node)getSession().get("HistoricalDocument", historyId);
        getSession().evict(historicalNode);
        return historicalNode;
    }

    public void persistHistoricalNode(Node historicalNode) {
        // TODO: Ugh, concatenating class names to get the entity name?!
        getSession().persist("Historical"+historicalNode.getClass().getSimpleName(), historicalNode);
        getSession().flush();
        getSession().evict(historicalNode);
    }

    @SuppressWarnings({"unchecked"})
    public List<Node> findHistoricalNodes(Node node) {
        if (node == null) return null;
        return getSession().createQuery("select n from HistoricalNode n where n.nodeId = :nodeId order by n.revision desc")
                            .setParameter("nodeId", node.getId())
                            .list();
    }
    
    // Multi-row constraint validation
    public boolean isUniqueWikiname(Node node) {
        Node foundNode = findNodeInArea(node.getParent().getAreaNumber(), node.getWikiname(), entityManager);
        if (foundNode == null) {
            return true;
        } else {
            return node.getId() != null && node.getId().equals(foundNode.getId());
        }
    }

    public Document findDocument(Long documentId) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Document) restrictedEntityManager
                    .createQuery("select d from Document d where d.id = :id")
                    .setParameter("id", documentId)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Directory findDirectory(Long directoryId) {
        restrictedEntityManager.joinTransaction();

        try {
            return (Directory) restrictedEntityManager
                    .createQuery("select d from Directory d where d.id = :id")
                    .setParameter("id", directoryId)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public File findFile(Long fileId) {
        restrictedEntityManager.joinTransaction();

        try {
            return (File) restrictedEntityManager
                    .createQuery("select f from File f where f.id = :id")
                    .setParameter("id", fileId)
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    public Document findDefaultDocument(Directory directory) {
        if (directory == null) return null;
        restrictedEntityManager.joinTransaction();
        try {
            return (Document) restrictedEntityManager
                    .createQuery("select doc from Document doc, Directory dir" +
                                 " where doc.id = dir.defaultDocument.id and dir.id = :did")
                    .setParameter("did", directory.getId())
                    .setHint("org.hibernate.cacheable", true)
                    .getSingleResult();
        } catch (EntityNotFoundException ex) {
        } catch (NoResultException ex) {
        }
        return null;
    }

    /*
    http://lucene.apache.org/java/docs/queryparsersyntax.html

    http://www.atlassian.com/software/jira/docs/v3.8/querysyntax.html
     */
    public List<Node> search(String searchTerm) throws ParseException {

        // Remove () parenthesis
        searchTerm = searchTerm.replaceAll("\\(", "\\(");
        searchTerm = searchTerm.replaceAll("\\)", "\\)");

        FullTextSession session = Search.createFullTextSession(getSession());
        QueryParser parser = new QueryParser("Document", new StandardAnalyzer());
        org.apache.lucene.search.Query query = parser.parse("name:(" + searchTerm + ") OR content:(" + searchTerm + ")");
        //noinspection unchecked
        return session.createFullTextQuery(query).list();
    }
    
    public Map<Long,Long> findCommentCount(Directory directory) {
        //noinspection unchecked
        List<Object[]> result = restrictedEntityManager
                .createQuery("select n.nodeId, count(c) from Node n, Comment c where c.document = n and n.parent is :parent group by n.nodeId")
                .setParameter("parent", directory)
                .getResultList();

        Map<Long,Long> resultMap = new HashMap<Long,Long>(result.size());
        for (Object[] objects : result) {
            resultMap.put((Long)objects[0], (Long)objects[1]);
        }
        return resultMap;
    }

    public <N extends Node> List<N> findWithParent(Class<N> nodeType, Directory directory, Node ignoreNode,
                                                   String orderByProperty, boolean orderDescending, long firstResult, long maxResults) {

        Criteria crit = prepareCriteria(nodeType, orderByProperty, orderDescending);
        crit.add(Restrictions.eq("parent", directory));
        if (ignoreNode != null)
            crit.add(Restrictions.ne("id", ignoreNode.getId()));
        if ( !(firstResult == 0 && maxResults == 0) )
            crit.setFirstResult(Long.valueOf(firstResult).intValue()).setMaxResults(Long.valueOf(maxResults).intValue());
        //noinspection unchecked
        return crit.list();
    }

    public int getRowCountWithParent(Class nodeType, Directory directory, Node ignoreNode) {
        Criteria crit = prepareCriteria(nodeType, null, false);
        crit.add(Restrictions.eq("parent", directory));
        if (ignoreNode != null)
            crit.add(Restrictions.ne("id", ignoreNode.getId()));
        return getRowCount(crit);
    }

    public <N extends Node> int getRowCountByExample(N exampleNode, String... ignoreProperty) {
        Criteria crit = prepareCriteria(exampleNode.getClass(), null, false);
        appendExample(crit, exampleNode, ignoreProperty);
        return getRowCount(crit);
    }

    private int getRowCount(Criteria criteria) {
        ScrollableResults cursor = criteria.scroll();
        cursor.last();
        int count = cursor.getRowNumber() + 1;
        cursor.close();
        return count;
    }

    private Criteria prepareCriteria(Class rootClass, String orderByProperty, boolean orderDescending) {
        Criteria crit = getSession().createCriteria(rootClass);
        if (orderByProperty != null)
                crit.addOrder( orderDescending ? Order.desc(orderByProperty) : Order.asc(orderByProperty) );
        return crit.setResultTransformer(new DistinctRootEntityResultTransformer());
    }

    private <N extends Node> void appendExample(Criteria criteria, N exampleNode, String... ignoreProperty) {
        Example example =  Example.create(exampleNode).enableLike(MatchMode.ANYWHERE).ignoreCase();
        for (String s : ignoreProperty) example.excludeProperty(s);
        criteria.add(example);
    }

    private Session getSession() {
        restrictedEntityManager.joinTransaction();
        return ((Session)((org.jboss.seam.persistence.EntityManagerProxy) restrictedEntityManager).getDelegate());
    }
}
