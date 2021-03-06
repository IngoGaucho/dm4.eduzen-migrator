package de.deepamehta.plugins.eduzenmigrator;

import de.deepamehta.plugins.eduzenmigrator.migrations.*;

import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.event.AllPluginsActiveListener;

import java.util.logging.Logger;



public class EduzenMigratorPlugin extends PluginActivator implements AllPluginsActiveListener {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void allPluginsActive() {
        // new M1SetupACLEntries(dms).run();
        // new M2SetAssocValues(dms).run();
        // new M3SetAssocInstValues(dms).run();
        new M4NewACLStorage(dms).run();
    }
}
