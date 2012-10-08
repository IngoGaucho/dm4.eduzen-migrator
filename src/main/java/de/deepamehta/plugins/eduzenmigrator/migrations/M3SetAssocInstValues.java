package de.deepamehta.plugins.eduzenmigrator.migrations;

import de.deepamehta.core.Association;
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

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;



public class M3SetAssocInstValues {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    public M3SetAssocInstValues(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void run() {
        logger.info("####################### EduZEN Migrator: Association Type Instantiations #######################");
        for (String assocTypeUri : dms.getAssociationTypeUris()) {
            logger.info("########## Association Type \"" + assocTypeUri + "\" ##########");
            setupAssociations(dms.getAssociations(assocTypeUri));
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupAssociations(Set<RelatedAssociation> relAssocs) {
        int totalAssoc = relAssocs.size();
        logger.info(totalAssoc + " instances");
        int a = 0;
        for (RelatedAssociation relAssoc : relAssocs) {
            DeepaMehtaTransaction tx = dms.beginTx();
            try {
                Association assoc = relAssoc.getRelatingAssociation();
                assoc.setSimpleValue("");
                a++;
                if (a % 100 == 0) {
                    logger.info("### " + a + "/" + totalAssoc);
                }
                tx.success();
            } catch (Exception e) {
                logger.warning("ROLLBACK!");
                throw new RuntimeException("EduZEN Migrator failed", e);
            } finally {
                tx.finish();
            }
        }
    }
}
