package de.deepamehta.plugins.eduzenmigrator.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedAssociation;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Role;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicRole;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.AssociationRoleModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.DeepaMehtaService;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class M1SetupACLEntries {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static long WORKSPACE_ID = 9676;

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Topic adminUsername;

    private int workspacesAssigned;
    private int aclCreated;

    private DeepaMehtaService dms;
    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    public M1SetupACLEntries(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void run() {
        prepare();
        //
        logger.info("############################## EduZEN Migrator: Compositions ##############################");
        setupAssociations(dms.getAssociations("dm4.core.composition"));
        //
        logger.info("############################## EduZEN Migrator: Aggregations ##############################");
        setupAssociations(dms.getAssociations("dm4.core.aggregation"));
        //
        logger.info("############################## EduZEN Migrator: Compatibles ##############################");
        setupAssociations(dms.getAssociations("tub.eduzen.compatible"));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void prepare() {
        // get admin user
        adminUsername = dms.getTopic("dm4.accesscontrol.username", new SimpleValue("admin"), false, null);
        if (adminUsername == null) {
            throw new RuntimeException("admin user not found");
        }
        // check workspace
        if (!dms.getTopic(WORKSPACE_ID, false, null).getTypeUri().equals("dm4.workspaces.workspace")) {
            throw new RuntimeException(WORKSPACE_ID + " is not a workspace");
        }
    }

    // ---

    private void setupAssociations(Set<? extends Association> assocs) {
        int totalAssoc = assocs.size();
        int ownAssoc = 0;
        workspacesAssigned = 0;
        aclCreated = 0;
        int a = 0;
        for (Association assoc : assocs) {
            if (isOwnAssociation(assoc)) {
                setupAssociation(assoc);
                ownAssoc++;
            }
            a++;
            if (a % 10 == 0) {
                logger.info("### " + a + "/" + totalAssoc + " (" + ownAssoc + " EduZEN, " +
                    workspacesAssigned + " Workspaces, " + aclCreated + " ACL)");
            }
        }
    }

    private void setupAssociation(Association assoc) {
        assignWorkspace(assoc);
        assignCreatorAndACL(assoc);
    }

    // === Workspace ===

    private void assignWorkspace(Association assoc) {
        if (hasWorkspace(assoc)) {
            return;
        }
        dms.createAssociation(new AssociationModel("dm4.core.aggregation",
            new AssociationRoleModel(assoc.getId(), "dm4.core.whole"),
            new TopicRoleModel(WORKSPACE_ID, "dm4.core.part")
        ), null);
        workspacesAssigned++;
    }

    private boolean hasWorkspace(Association assoc) {
        return assoc.getRelatedTopics("dm4.core.aggregation", "dm4.core.whole", "dm4.core.part",
            "dm4.workspaces.workspace", false, false, 0, null).getSize() > 0;
    }

    // === Creator ===

    private void assignCreatorAndACL(Association assoc) {
        if (hasCreator(assoc)) {
            return;
        }
        //
        Topic creator = dms.createTopic(new TopicModel("dm4.accesscontrol.creator", new CompositeValue()
            .putRef("dm4.accesscontrol.username", adminUsername.getId())), null);
        // assign creator to assoc
        dms.createAssociation(new AssociationModel("dm4.core.composition",
            new AssociationRoleModel(assoc.getId(), "dm4.core.whole"),
            new TopicRoleModel(creator.getId(), "dm4.core.part")
        ), null);
        //
        assignACLEntry(assoc);
    }

    private boolean hasCreator(Association assoc) {
        return assoc.getRelatedTopics("dm4.core.composition", "dm4.core.whole", "dm4.core.part",
            "dm4.accesscontrol.creator", false, false, 0, null).getSize() > 0;
    }

    // === ACL Entry ===

    private void assignACLEntry(Association assoc) {
        Topic aclEntry = dms.createTopic(new TopicModel("dm4.accesscontrol.acl_entry", new CompositeValue()
            .putRef("dm4.accesscontrol.user_role", "dm4.accesscontrol.user_role.member")
            .add("dm4.accesscontrol.permission", new TopicModel("dm4.accesscontrol.permission", new CompositeValue()
                .putRef("dm4.accesscontrol.operation", "dm4.accesscontrol.operation.write")
                .put("dm4.accesscontrol.allowed", true)
            ))
        ), null);
        // assign ACL entry to assoc
        dms.createAssociation(new AssociationModel("dm4.core.composition",
            new AssociationRoleModel(assoc.getId(), "dm4.core.whole"),
            new TopicRoleModel(aclEntry.getId(), "dm4.core.part")
        ), null);
        aclCreated++;
    }

    // === Helper ===

    private boolean isOwnTopic(Topic topic) {
        return isOwnUri(topic.getTypeUri());
    }

    private boolean isOwnAssociation(Association assoc) {
        return isOwnRole(assoc.getRole1()) && isOwnRole(assoc.getRole2());            
    }

    private boolean isOwnRole(Role role) {
        if (!(role instanceof TopicRole)) {
            return false;
        }
        Topic topic = ((TopicRole) role).getTopic();
        return isOwnTopic(topic);
    }

    private boolean isOwnUri(String uri) {
        return uri.startsWith("tub.eduzen.");
    }
}
