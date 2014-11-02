package cloudx.view.messagebox;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import cloudx.main.R;

import java.io.IOException;

/**
 * Created by Gongpu on 2014/4/25.
 */
public class MessageBox {

    private Context context = null;
    private EditText editText = null;

    private MessageSender sender = null;

    private String content = null;

    /**
     * 用于发送
     *
     * @param context
     * @param sender
     */
    public MessageBox(Context context, MessageSender sender) {
        this.context = context;
        this.sender = sender;
    }

    /**
     * 用于接收
     *
     * @param context
     */
    public MessageBox(Context context, String content) {
        this.context = context;
        this.content = content;
    }

    public void show() {

        editText = new EditText(context);

        if (editText.hasFocus())
            editText.clearFocus();

        if (sender != null) {
            editText.setImeOptions(EditorInfo.IME_ACTION_SEND);
            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        checkAndSend();
                        return true;
                    }

                    return false;
                }
            });

            new AlertDialog.Builder(context)
                    .setTitle(R.string.PleaseInputMessage)
                    .setView(editText)
                    .setPositiveButton(R.string.Send, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            checkAndSend();
                        }
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .create().show();
        } else {
            EditText editText = new EditText(context);
            editText.setText(content);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.GetAMessage)
                    .setView(editText)
                    .setPositiveButton(context.getText(R.string.Confirm), null).create().show();
        }
    }


    private void checkAndSend() {
        if (editText != null)
            if (editText.getText() != null && editText.getText().length() > 0) {
                try {
                    sender.sendMessage(editText.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                editText.setError(context.getString(R.string.ContentCannotBeNull));
                editText.requestFocus();//提示错误
            }
    }

}
