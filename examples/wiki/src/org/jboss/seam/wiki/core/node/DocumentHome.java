package org.jboss.seam.wiki.core.node;

import static javax.faces.application.FacesMessage.SEVERITY_ERROR;

import org.jboss.seam.framework.EntityHome;
import org.jboss.seam.annotations.*;
import org.jboss.seam.wiki.core.links.WikiLinkResolver;
import org.jboss.seam.wiki.core.dao.NodeDAO;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.ScopeType;

@Name("documentHome")
public class DocumentHome extends EntityHome<Document> {

    @RequestParameter
    private Long docId;

    @RequestParameter
    private Long parentDirId;

    // Pages need this for rendering
    @Out(required = true, scope = ScopeType.CONVERSATION, value = "currentDirectory")
    Directory parentDirectory;

    @In
    private FacesMessages facesMessages;

    @In
    private NodeBrowser browser;

    @In
    private WikiLinkResolver wikiLinkResolver;

    @In
    private NodeDAO nodeDAO;

    private String formContent;
    boolean enabledPreview = false;

    @Override
    public Object getId() {

        if (docId == null) {
            return super.getId();
        } else {
            return docId;
        }
    }

    @Override
    @Transactional
    public void create() {
        super.create();

        // Load the parent directory
        getEntityManager().joinTransaction();
        parentDirectory = getEntityManager().find(Directory.class, parentDirId);
    }

    // TODO: Typical exit method to get out of a root or nested conversation, JBSEAM-906
    public void exitConversation(Boolean endBeforeRedirect) {
        Conversation currentConversation = Conversation.instance();
        if (currentConversation.isNested()) {
            // End this nested conversation and return to last rendered view-id of parent
            currentConversation.endAndRedirect(endBeforeRedirect);
        } else {
            // End this root conversation
            currentConversation.end();
            // Return to the view-id that was captured when this conversation started
            if (endBeforeRedirect)
                browser.redirectToLastBrowsedPage();
            else
                browser.redirectToLastBrowsedPageWithConversation();
        }
    }

    @Override
    public String persist() {

        // Validate
        if (!isUniqueWikinameInDirectory() ||
            !isUniqueWikinameInArea()) return null;

        // Link the document with a directory
        parentDirectory.addChild(getInstance());

        // Set its area number
        getInstance().setAreaNumber(parentDirectory.getAreaNumber());

        // Convert and set form content onto entity instance
        getInstance().setContent(
            wikiLinkResolver.convertToWikiLinks(parentDirectory, getFormContent())
        );

        return super.persist();
    }


    @Override
    public String update() {

        // Validate
        if (!isUniqueWikinameInDirectory() ||
            !isUniqueWikinameInArea()) return null;

        // Convert and set form content onto entity instance
        getInstance().setContent(
            wikiLinkResolver.convertToWikiLinks(parentDirectory, getFormContent())
        );

        Events.instance().raiseEvent("Nodes.menuStructureModified");

        return super.update();
    }

    @Override
    public String remove() {

        // Unlink the document from its directory
        getInstance().getParent().removeChild(getInstance());

        Events.instance().raiseEvent("Nodes.menuStructureModified");

        return super.remove();
    }

    public String getFormContent() {
        // Load the document content and resolve links
        if (formContent == null)
            formContent = wikiLinkResolver.convertFromWikiLinks(parentDirectory, getInstance().getContent());
        return formContent;
    }

    public void setFormContent(String formContent) {
        this.formContent = formContent;
    }

    public boolean isEnabledPreview() {
        return enabledPreview;
    }

    public void setEnabledPreview(boolean enabledPreview) {
        this.enabledPreview = enabledPreview;
        // Convert and set form content onto entity instance
        getInstance().setContent(
            wikiLinkResolver.convertToWikiLinks(parentDirectory, getFormContent())
        );
    }

    // Validation rules for persist(), update(), and remove();

    private boolean isUniqueWikinameInDirectory() {
        Node foundNode = nodeDAO.findNodeInDirectory(parentDirectory, getInstance().getWikiname());
        if (foundNode != null && foundNode != getInstance()) {
            facesMessages.addToControlFromResourceBundleOrDefault(
                "name",
                SEVERITY_ERROR,
                getMessageKeyPrefix() + "duplicateName",
                "This name is already used, please change it."
            );
            return false;
        }
        return true;
    }

    private boolean isUniqueWikinameInArea() {
        Node foundNode = nodeDAO.findNodeInArea(parentDirectory.getAreaNumber(), getInstance().getWikiname());
        if (foundNode != null && foundNode != getInstance()) {
            facesMessages.addToControlFromResourceBundleOrDefault(
                "name",
                SEVERITY_ERROR,
                getMessageKeyPrefix() + "duplicateNameInArea",
                "This name is already used in this area, please change it."
            );
            return false;
        }
        return true;
    }

}
