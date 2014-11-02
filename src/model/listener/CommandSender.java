package model.listener;

import common.message.Data;

/**
 * Created by Gongpu on 2014/3/31.
 */
public interface CommandSender {
    public void sendCommand(Data.Command.CommandType commandType, float x, float y);
}
