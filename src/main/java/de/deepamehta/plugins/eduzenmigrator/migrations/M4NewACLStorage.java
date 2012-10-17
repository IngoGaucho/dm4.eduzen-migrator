package de.deepamehta.plugins.eduzenmigrator.migrations;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.DeepaMehtaTransaction;
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
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.accesscontrol.AccessControlList;
import de.deepamehta.core.service.accesscontrol.ACLEntry;
import de.deepamehta.core.service.accesscontrol.Operation;
import de.deepamehta.core.service.accesscontrol.UserRole;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class M4NewACLStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final AccessControlList DEFAULT_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );
    private static final AccessControlList DEFAULT_TYPE_ACL = new AccessControlList(
        new ACLEntry(Operation.WRITE,  UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER),
        new ACLEntry(Operation.CREATE, UserRole.CREATOR, UserRole.OWNER, UserRole.MEMBER)
    );

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    public M4NewACLStorage(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void run() {
        logger.info("####################### EduZEN Migrator: New ACL Storage #######################");
        ResultSet<Topic> aclTopics = dms.getTopics("dm4.accesscontrol.acl_entry", false, 0, null);
        transformACLEntries(aclTopics.getItems());
        deleteACLTypes();
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void transformACLEntries(Set<Topic> aclTopics) {
        int total = aclTopics.size();
        int a = 0;
        for (Topic aclTopic : aclTopics) {
            if (a % 100 == 0) {
                logger.info("### " + a + "/" + total);
            }
            //
            DeepaMehtaObject dmo = parent(aclTopic);
            setupAccessControl(dmo);
            //
            a++;
        }
    }

    private DeepaMehtaObject parent(Topic topic) {
        DeepaMehtaObject parent = topic.getRelatedTopic("dm4.core.composition", "dm4.core.part", "dm4.core.whole",
            null, false, false, null);
        if (parent != null) {
            return parent;
        }
        //
        parent = topic.getRelatedAssociation("dm4.core.composition", "dm4.core.part", "dm4.core.whole",
            null, false, false);
        if (parent != null) {
            return parent;
        }
        //
        throw new RuntimeException("ACL Entry has no parent (" + topic + ")");
    }

    private void setupAccessControl(DeepaMehtaObject dmo) {
        dms.createACL(dmo.getId(), isType(dmo) ? DEFAULT_TYPE_ACL : DEFAULT_ACL);
        dms.setCreator(dmo.getId(), "admin");
        dms.setOwner(dmo.getId(), "admin");
    }

    private boolean isType(DeepaMehtaObject dmo) {
        return dmo.getTypeUri().equals("dm4.core.topic_type") ||
               dmo.getTypeUri().equals("dm4.core.assoc_type");
    }

    // ---

    private void deleteACLTypes() {
        deleteType("dm4.accesscontrol.acl_entry");
        deleteType("dm4.accesscontrol.operation");
        deleteType("dm4.accesscontrol.allowed");
        deleteType("dm4.accesscontrol.permission");
        deleteType("dm4.accesscontrol.user_role");
        deleteType("dm4.accesscontrol.acl_facet");
        deleteType("dm4.accesscontrol.creator");
        deleteType("dm4.accesscontrol.creator_facet");
        deleteType("dm4.accesscontrol.owner");
        deleteType("dm4.accesscontrol.owner_facet");
    }

    private void deleteType(String typeUri) {
        // delete instances
        Set<Topic> topics = dms.getTopics(typeUri, false, 0, null).getItems();
        logger.info("### Deleting " + topics.size() + " " + typeUri + " topics");
        for (Topic topic : topics) {
            try {
                topic.delete(new Directives());
            } catch (Exception e) {
                logger.info("########## WARNING: topic " + topic.getId() + " can not be deleted (" + e + ")");
            }
        }
        // check
        topics = dms.getTopics(typeUri, false, 0, null).getItems();
        if (topics.size() != 0) {
            logger.info("########## WARNING: " + topics.size() + " " + typeUri + " instances left");
        }
        // delete type
        logger.info("### Deleting topic type " + typeUri);
        dms.getTopicType(typeUri, null).delete(new Directives());
    }
}
