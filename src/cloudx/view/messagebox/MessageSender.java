package cloudx.view.messagebox;

import java.io.IOException;

/**
 * Created by Gongpu on 2014/4/25.
 */
public interface MessageSender {
    public void sendMessage(String message) throws IOException;
}
