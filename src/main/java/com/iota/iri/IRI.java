package com.iota.iri;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iota.iri.conf.Configuration;
import com.iota.iri.conf.Configuration.DefaultConfSettings;
import com.iota.iri.service.API;

/**
 * Main IOTA Reference Implementation starting class
 */
public class IRI {

    private static final Logger log = LoggerFactory.getLogger(IRI.class);

    public static Iota iota;
    public static API api;
    public static IXI ixi;
    public static Configuration conf;

    public static void main(final String[] args) throws IOException {
        conf = new Configuration();
        conf.validateParams(log, args);
        iota = new Iota(conf);
        ixi = new IXI(iota);
        api = new API(iota, ixi);
        shutdownHook();

        if (conf.booling(DefaultConfSettings.DEBUG)) {
            log.info("You have set the debug flag. To enable debug output, you need to uncomment the DEBUG appender in the source tree at iri/src/main/resources/logback.xml and re-package iri.jar");
        }

        if (conf.booling(DefaultConfSettings.EXPORT)) {
            File exportDir = new File("export");
            // if the directory does not exist, create it
            if (!exportDir.exists()) {
                log.info("Create directory 'export'");
                try {
                    exportDir.mkdir();
                } catch (SecurityException e) {
                    log.error("Could not create directory",e);
                }
            }
            exportDir = new File("export-solid");
            // if the directory does not exist, create it
            if (!exportDir.exists()) {
                log.info("Create directory 'export-solid'");
                try {
                    exportDir.mkdir();
                } catch (SecurityException e) {
                    log.error("Could not create directory",e);
                }
            }
        }

        try {
            iota.init();
            api.init();
            ixi.init(conf.string(DefaultConfSettings.IXI_DIR));
        } catch (final Exception e) {
            log.error("Exception during IOTA node initialisation: ", e);
            System.exit(-1);
        }
        log.info("IOTA Node initialised correctly.");
    }


    private static void shutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            log.info("Shutting down IOTA node, please hold tight...");
            try {
                ixi.shutdown();
                api.shutDown();
                iota.shutdown();
            } catch (final Exception e) {
                log.error("Exception occurred shutting down IOTA node: ", e);
            }
        }, "Shutdown Hook"));
    }
}
