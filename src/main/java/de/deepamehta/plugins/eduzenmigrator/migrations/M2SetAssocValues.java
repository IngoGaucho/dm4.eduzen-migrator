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



public class M2SetAssocValues {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------- Constructor

    public M2SetAssocValues(DeepaMehtaService dms) {
        this.dms = dms;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public void run() {
        logger.info("############################# EduZEN Migrator: Association Values #############################");
        setupAssociations(dms.getAssociations("dm4.core.instantiation"));
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void setupAssociations(Set<? extends Association> assocs) {
        int totalAssoc = assocs.size();
        int a = 0;
        for (Association assoc : assocs) {
            DeepaMehtaTransaction tx = dms.beginTx();
            try {
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
