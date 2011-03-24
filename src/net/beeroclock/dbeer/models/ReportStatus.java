package net.beeroclock.dbeer.models;

/**
 * Very rough holder for the server reply to a pricing report
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-03-24
 */
public class ReportStatus {
    public boolean success;
    public String serverMessage;

    public ReportStatus(boolean success, String serverMessage) {
        this.success = success;
        this.serverMessage = serverMessage;
    }
}
