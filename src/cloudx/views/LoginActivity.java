package cloudx.views;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import cloudx.model.User;
import cloudx.model.Constants;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class LoginActivity extends Activity {

    private EditText accountEditText = null;
    private EditText passwordEditText = null;

    private Button loginButton = null;
    private Button registerButton = null;
    private Button forgotPasswordButton = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //如果已经存储账号密码，则直接登录
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SharedPreferenceName, MODE_PRIVATE);
        String account = sharedPreferences.getString(Constants.SharedPreference_Key_Account, null);
        String password = sharedPreferences.getString(Constants.SharedPreference_Key_Password, null);
        if (account != null && password != null) {
            User.login(account, password);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.login_layout);
        initViews();
    }

    /**
     * 初始化界面，并设置监听器
     */
    private void initViews() {
        accountEditText = (EditText) findViewById(R.id.account);
        passwordEditText = (EditText) findViewById(R.id.password);
        loginButton = (Button) findViewById(R.id.login);
        registerButton = (Button) findViewById(R.id.register);
        forgotPasswordButton = (Button) findViewById(R.id.forgot_password);


        //set listeners

        loginButton.setOnClickListener(new View.OnClickListener() {
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

                //TODO 登录
                String account = accountEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                //存储账号密码
                SharedPreferences.Editor editor = getSharedPreferences(Constants.SharedPreferenceName, MODE_PRIVATE).edit();
                editor.putString(Constants.SharedPreference_Key_Account, account);
                editor.putString(Constants.SharedPreference_Key_Password, password);
                editor.apply();

                User.login(account, password);

            }
        });

        //跳转到 register activity
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });

        //跳转到 Forgot Password Activity
        forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO
            }
        });
    }
}