package ioio.lib.api;

import java.io.IOException;

import ioio.lib.api.exception.ConnectionLostException;

/**
 * Created by johnlam on 5/01/15.
 */
public interface PID extends Closeable {

    public float getSpeed();

    public void setSpeed(float speed) throws IOException;

    public void setParam(float P,float I,float D) throws IOException;

}
