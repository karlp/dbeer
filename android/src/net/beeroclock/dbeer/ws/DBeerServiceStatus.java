package net.beeroclock.dbeer.ws;

/**
 * A web service response container for reading the server status report
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-04-14
 */
public class DBeerServiceStatus {
    public boolean success;
    public Throwable exception;
    public String message;
    public String lastUpdated;

    public DBeerServiceStatus(String lastUpdated) {
        success = true;
        this.lastUpdated = lastUpdated;
    }

    public DBeerServiceStatus(String message, Throwable exception) {
        this.success = false;
        this.message = message;
        this.exception = exception;
    }

}
