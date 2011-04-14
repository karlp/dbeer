package net.beeroclock.dbeer.ws;

import net.beeroclock.dbeer.models.Bar;

import java.util.Set;

/**
* Represents the result of calling the server for a list of bars
*
* @author Karl Palsson, 2011
*         Date: 2011-04-14
*/
public class BarServiceFetcherResult {
    public boolean success;
    public Throwable exception;
    public String message;
    public Set<Bar> bars;

    public BarServiceFetcherResult(Set<Bar> bars) {
        success = true;
        this.bars = bars;
    }

    public BarServiceFetcherResult(String message) {
        this.success = false;
        this.message = message;
    }

    public BarServiceFetcherResult(String message, Throwable exception) {
        this.success = false;
        this.message = message;
        this.exception = exception;
    }
}
