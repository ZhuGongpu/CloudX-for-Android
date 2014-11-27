package cloudx.views;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import cloudx.model.User;
import data.information.Constants;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class RegisterActivity extends Activity {
    private EditText accountEditText = null;
    private EditText passwordEditText = null;
    private EditText confirmPasswordEditText = null;

    private Button registerButton = null;

    /**
     * 判断输入密码是否合法
     *
     * @param password
     * @return
     */
    private static boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        initViews();
    }

    /**
     * 初始化界面元素，并设置监听器
     */
    private void initViews() {
        accountEditText = (EditText) findViewById(R.id.account);
        passwordEditText = (EditText) findViewById(R.id.password);
        confirmPasswordEditText = (EditText) findViewById(R.id.confirm_password);

        registerButton = (Button) findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //check
                if (accountEditText.getText() == null) {
                    accountEditText.setError(getText(R.string.account_cannot_be_empty));
                    return;
                }

                if (passwordEditText.getText() == null) {
                    passwordEditText.setError(getText(R.string.password_cannot_be_empty));
                    return;
                } else if (passwordEditText.getText().length() < 6) {
                    passwordEditText.setError(getText(R.string.password_too_short));
                    return;
                }

                if (confirmPasswordEditText.getText() == null) {
                    confirmPasswordEditText.setError(getText(R.string.confirm_password));
                    return;
                } else if (!confirmPasswordEditText.getText().toString().equals(passwordEditText.getText().toString())) {
                    confirmPasswordEditText.setError(getText(R.string.passwords_donot_match));
                    return;
                }

                //TODO 注册并登录

                String account = accountEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                //存储账号密码
                SharedPreferences.Editor editor = getSharedPreferences(Constants.SharedPreferenceName, MODE_PRIVATE).edit();
                editor.putString(Constants.SharedPreference_Key_Account, account);
                editor.putString(Constants.SharedPreference_Key_Password, password);
                editor.apply();

                //注册
                User.register(account, password);

                //登录
                User.login(account, password);
            }
        });

    }


}