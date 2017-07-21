package com.iota.iri.conf;

import org.apache.commons.lang3.StringUtils;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanityinc.jargs.CmdLineParser;
import com.sanityinc.jargs.CmdLineParser.Option;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;

/**
 * All those settings are modifiable at runtime,
 * but for most of them the node needs to be restarted.
 */
public class Configuration {
    private Ini ini;
    private Preferences prefs;

    public static final String MAINNET_NAME = "IRI";
    public static final String TESTNET_NAME = "IRI Testnet";
    public static final String VERSION = "1.2.1";

    private final Logger log = LoggerFactory.getLogger(Configuration.class);

    private final Map<String, String> conf = new ConcurrentHashMap<>();

    public enum DefaultConfSettings {
        CONFIG,
        PORT,
        API_HOST,
        UDP_RECEIVER_PORT,
        TCP_RECEIVER_PORT,
        TESTNET,
        DEBUG,
        REMOTE_LIMIT_API,
        REMOTE_AUTH,
        NEIGHBORS,        
        IXI_DIR,
        DB_PATH,
        DB_LOG_PATH,
        P_REMOVE_REQUEST,
        P_DROP_TRANSACTION,
        P_SELECT_MILESTONE_CHILD,
        P_SEND_MILESTONE,
        MAIN_DB, 
        EXPORT, // exports transaction trytes to filesystem
        SEND_LIMIT,
        MAX_PEERS,
        COORDINATOR,
        REVALIDATE,
        RESCAN_DB,
        MAX_RANDOM_WALKS,
        MAX_FIND_TRANSACTIONS,
        MAX_GET_TRYTES,
        MAX_DEPTH,
        MAINNET_MWM,
        TESTNET_MWM,
    }

    {
        // defaults
        conf.put(DefaultConfSettings.PORT.name(), "14600");
        conf.put(DefaultConfSettings.API_HOST.name(), "localhost");
        conf.put(DefaultConfSettings.UDP_RECEIVER_PORT.name(), "14600");
        conf.put(DefaultConfSettings.TCP_RECEIVER_PORT.name(), "15600");
        conf.put(DefaultConfSettings.TESTNET.name(), "false");
        conf.put(DefaultConfSettings.DEBUG.name(), "false");
        conf.put(DefaultConfSettings.REMOTE_LIMIT_API.name(), "");
        conf.put(DefaultConfSettings.REMOTE_AUTH.name(), "");
        conf.put(DefaultConfSettings.NEIGHBORS.name(), "");
        conf.put(DefaultConfSettings.IXI_DIR.name(), "ixi");
        conf.put(DefaultConfSettings.DB_PATH.name(), "mainnetdb");
        conf.put(DefaultConfSettings.DB_LOG_PATH.name(), "mainnet.log");
        conf.put(DefaultConfSettings.CONFIG.name(), "iota.ini");
        conf.put(DefaultConfSettings.P_REMOVE_REQUEST.name(), "0.01");
        conf.put(DefaultConfSettings.P_DROP_TRANSACTION.name(), "0.0");
        conf.put(DefaultConfSettings.P_SELECT_MILESTONE_CHILD.name(), "0.7");
        conf.put(DefaultConfSettings.P_SEND_MILESTONE.name(), "0.02");
        conf.put(DefaultConfSettings.MAIN_DB.name(), "rocksdb");
        conf.put(DefaultConfSettings.EXPORT.name(), "false");
        conf.put(DefaultConfSettings.SEND_LIMIT.name(), "-1.0");
        conf.put(DefaultConfSettings.MAX_PEERS.name(), "0");
        conf.put(DefaultConfSettings.REVALIDATE.name(), "false");
        conf.put(DefaultConfSettings.RESCAN_DB.name(), "false");
        conf.put(DefaultConfSettings.MAINNET_MWM.name(), "15");
        conf.put(DefaultConfSettings.TESTNET_MWM.name(), "13");
        conf.put(DefaultConfSettings.MAX_RANDOM_WALKS.name(), "27"); // Pick a number based on best performance
        conf.put(DefaultConfSettings.MAX_DEPTH.name(), "15");        // Pick a milestone depth number depending on risk model
        conf.put(DefaultConfSettings.MAX_FIND_TRANSACTIONS.name(), "100000");
        conf.put(DefaultConfSettings.MAX_GET_TRYTES.name(), "10000");

    }

    public void validateParams(Logger log, final String[] args) throws IOException {

        boolean configurationInit = this.init();
        
        if (args == null || (args.length < 2 && !configurationInit)) {
            log.error("Invalid arguments list. Provide ini-file 'iota.ini' or API port number (i.e. '-p 14600').");
            printUsage(log);
        }

        final CmdLineParser parser = new CmdLineParser();

        final Option<String> config = parser.addStringOption('c', "config");
        final Option<String> port = parser.addStringOption('p', "port");
        final Option<String> rportudp = parser.addStringOption('u', "udp-receiver-port");
        final Option<String> rporttcp = parser.addStringOption('t', "tcp-receiver-port");
        final Option<Boolean> debug = parser.addBooleanOption('d', "debug");
        final Option<Boolean> remote = parser.addBooleanOption("remote");
        final Option<String> remoteLimitApi = parser.addStringOption("remote-limit-api");
        final Option<String> remoteAuth = parser.addStringOption("remote-auth");
        final Option<String> neighbors = parser.addStringOption('n', "neighbors");
        final Option<Boolean> export = parser.addBooleanOption("export");
        final Option<Boolean> help = parser.addBooleanOption('h', "help");
        final Option<Boolean> testnet = parser.addBooleanOption("testnet");
        final Option<Boolean> revalidate = parser.addBooleanOption("revalidate");
        final Option<Boolean> rescan = parser.addBooleanOption("rescan");
        final Option<String> sendLimit = parser.addStringOption("send-limit");
        final Option<String> maxPeers = parser.addStringOption("max-peers");

        try {
            assert args != null;
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            log.error("CLI error: ", e);
            printUsage(log);
            System.exit(2);
        }

        // optional config file path
        String confFilePath = parser.getOptionValue(config);
        if(confFilePath != null ) {
            put(DefaultConfSettings.CONFIG, confFilePath);
            init();
        }

        // mandatory args
        String inicport = this.getIniValue(DefaultConfSettings.PORT.name());
        final String cport = inicport == null ? parser.getOptionValue(port) : inicport;
        if (cport == null) {
            log.error("Invalid arguments list. Provide at least the PORT in iota.ini or with -p option");
            printUsage(log);
        }
        else {
            put(DefaultConfSettings.PORT, cport);
        }

        // optional flags
        if (parser.getOptionValue(help) != null) {
            printUsage(log);
        }

        String cns = parser.getOptionValue(neighbors);
        if (cns == null) {
            log.warn("No neighbor has been specified. Server starting nodeless.");
            cns = StringUtils.EMPTY;
        }
        put(DefaultConfSettings.NEIGHBORS, cns);

        final String vremoteapilimit = parser.getOptionValue(remoteLimitApi);
        if (vremoteapilimit != null) {
            log.debug("The following api calls are not allowed : {} ", vremoteapilimit);
            put(DefaultConfSettings.REMOTE_LIMIT_API, vremoteapilimit);
        }
        
        final String vremoteauth = parser.getOptionValue(remoteAuth);
        if (vremoteauth != null) {
            log.debug("Remote access requires basic authentication");
            put(DefaultConfSettings.REMOTE_AUTH, vremoteauth);
        }

        final String vrportudp = parser.getOptionValue(rportudp);
        if (vrportudp != null) {
            put(DefaultConfSettings.UDP_RECEIVER_PORT, vrportudp);
        }
        
        final String vrporttcp = parser.getOptionValue(rporttcp);
        if (vrporttcp != null) {
            put(DefaultConfSettings.TCP_RECEIVER_PORT, vrporttcp);
        }

        if (parser.getOptionValue(remote) != null) {
            log.info("Remote access enabled. Binding API socket to listen any interface.");
            put(DefaultConfSettings.API_HOST, "0.0.0.0");
        }

        if (parser.getOptionValue(export) != null) {
            log.info("Export transaction trytes turned on.");
            put(DefaultConfSettings.EXPORT, "true");
        }

        if (Integer.parseInt(cport) < 1024) {
            log.warn("Warning: api port value seems too low.");
        }

        if (parser.getOptionValue(debug) != null) {
            put(DefaultConfSettings.DEBUG, "true");
            log.info(allSettings());
            StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());
        }

        if (parser.getOptionValue(testnet) != null) {
            put(DefaultConfSettings.TESTNET, "true");
            put(DefaultConfSettings.DB_PATH.name(), "testnetdb");
            put(DefaultConfSettings.DB_LOG_PATH.name(), "testnetdb.log");
        }

        if (parser.getOptionValue(revalidate) != null) {
            put(DefaultConfSettings.REVALIDATE, "true");
        }

        if (parser.getOptionValue(rescan) != null) {
            put(DefaultConfSettings.RESCAN_DB, "true");
        }

        final String vsendLimit = parser.getOptionValue(sendLimit);
        if (vsendLimit != null) {
            put(DefaultConfSettings.SEND_LIMIT, vsendLimit);
        }
        
        final String vmaxPeers = parser.getOptionValue(maxPeers);
        if (vmaxPeers != null) {
            put(DefaultConfSettings.MAX_PEERS, vmaxPeers);
        }

        log.info("Welcome to {} {}", booling(DefaultConfSettings.TESTNET) ? TESTNET_NAME : MAINNET_NAME, VERSION);
    }

    private static void printUsage(Logger log) {
        log.info("Usage: java -jar {}-{}.jar " +
                "[{-n,--neighbors} '<list of neighbors>'] " +
                "[{-p,--port} 14600] " +                
                "[{-c,--config} 'config-file-name'] " +
                "[{-u,--udp-receiver-port} 14600] " +
                "[{-t,--tcp-receiver-port} 15600] " +
                "[{-d,--debug} false] " +
                "[{--testnet} false]" +
                "[{--remote} false]" +
                "[{--remote-auth} string]" +
                "[{--remote-limit-api} string]"
                , MAINNET_NAME, VERSION);
        System.exit(0);
    }

    
    public boolean init() throws IOException {
        File confFile = new File(string(Configuration.DefaultConfSettings.CONFIG));
        if(confFile.exists()) {
            ini = new Ini(confFile);
            prefs = new IniPreferences(ini);
            return true;
        }
        return false;
    }

    public String getIniValue(String k) {
        if(ini != null) {
            return prefs.node("IRI").get(k, null);
        }
        return null;
    }

    private String getConfValue(String k) {
        String value = getIniValue(k);
        return value == null? conf.get(k): value;
    }

    public String allSettings() {
        final StringBuilder settings = new StringBuilder();
        conf.keySet().forEach(t -> settings.append("Set '").append(t).append("'\t -> ").append(getConfValue(t)).append("\n"));
        return settings.toString();
    }

    public void put(final String k, final String v) {
        log.debug("Setting {} with {}", k, v);
        put(k, v);
    }

    public void put(final DefaultConfSettings d, String v) {
        log.debug("Setting {} with {}", d.name(), v);
        conf.put(d.name(), v);
    }

    private String string(String k) {
        return getConfValue(k);
    }

    public float floating(String k) {
        return Float.parseFloat(getConfValue(k));
    }

    public double doubling(String k) {
        return Double.parseDouble(getConfValue(k));
    }

    private int integer(String k) {
        return Integer.parseInt(getConfValue(k));
    }

    private boolean booling(String k) {
        return Boolean.parseBoolean(getConfValue(k));
    }

    public String string(final DefaultConfSettings d) {
        return string(d.name());
    }

    public int integer(final DefaultConfSettings d) {
        return integer(d.name());
    }

    public boolean booling(final DefaultConfSettings d) {
        return booling(d.name());
    }
}
