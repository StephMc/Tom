package ioio.lib.impl;

import java.io.IOException;

import ioio.lib.api.PID;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.impl.IncomingState.InputPinListener;

/**
 * Created by johnlam on 5/01/15.
 */

public class PIDImpl extends AbstractResource implements PID, InputPinListener {

    private int chn;
    private float speed;

    public PIDImpl(IOIOImpl ioio, int chn) throws ConnectionLostException {
        super(ioio);
        this.chn = chn;
    }

    @Override
    public float getSpeed(){
        return speed;
    }

    @Override
    public void setSpeed(float speed) throws IOException {
        ioio_.protocol_.pidSet(chn,Math.round(speed * Short.MAX_VALUE));
    }

    @Override
    public void setParam(float P, float I, float D) throws IOException {
        ioio_.protocol_.pidConfig(chn,Math.round(P * Short.MAX_VALUE),Math.round(I * Short.MAX_VALUE), Math.round(D * Short.MAX_VALUE));
    }

    @Override
    public void close() {
        speed = 0;
    }

    @Override
    public void setValue(int value) {
        speed = ((float)((short)value))/Short.MIN_VALUE;
    }
}

