import Server.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StartServer {
    private static final Logger LOGGER = LogManager.getLogger(StartServer.class);
    public static void main(String[] args)  {
        LOGGER.debug("Debug");
        LOGGER.info("Info");
        LOGGER.warn("Warn");
        LOGGER.error("Error");
        LOGGER.fatal("Fatal");
        LOGGER.info("String: {}.", "Старт LOGGER");


        new Server();
    }

}