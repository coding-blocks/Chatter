package com.codingblocks.chatter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class AuthenticationActivity extends AppCompatActivity {

    final OkHttpClient client = new OkHttpClient();
    @BindView(R.id.authentication_web_view) WebView authenticationWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Exclude the title bar for this activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_authentication);
        ButterKnife.bind(this);

        // Begin authentication
        authenticationWebView.getSettings().setSupportZoom(true);
        authenticationWebView.getSettings().setDomStorageEnabled(true);
        authenticationWebView.getSettings().setJavaScriptEnabled(true);
        android.webkit.CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        authenticationWebView.setWebViewClient(new WebViewClient() {
            // shouldOverrideUrlLoading makes this `WebView` the default handler for URLs
            // inside the app, so that links are not kicked out to other apps.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // If authentication is successfull gitter API redirects the webbrowser
                //  to a preconfigured url (found in OauthKeys) with a parameter `code`
                if (url.contains(OauthKeys.redirectUri) && url.contains("?code=")) {
                    // We will extract the value of the parameter code
                    // Note: lenght + 7 is such that `?code=` will consume 6 indexes and the
                    // value we need beings from the 7th index
                    String authenticationCode = url.substring(OauthKeys.redirectUri.length()+6);
                    RequestBody requestBody = new FormBody.Builder()
                            .add("client_id", OauthKeys.clientID)
                            .add("client_secret", OauthKeys.clientSecret)
                            .add("code", authenticationCode)
                            .add("redirect_uri", OauthKeys.redirectUri)
                            .add("grant_type", "authorization_code")
                            .build();

                    Request request = new Request.Builder()
                            .url("https://gitter.im/login/oauth/token")
                            .method("POST", requestBody)
                            .addHeader("Accept", "application/json")
                            .post(requestBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try {
                                String responseText = response.body().string();
                                JSONObject Jobject = new JSONObject(responseText);
                                final String accessToken = Jobject.getString("access_token");
                                AuthenticationActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SharedPreferences sharedPreferences = AuthenticationActivity.this.
                                                getApplicationContext().getSharedPreferences("UserPreferences", 0);
                                        sharedPreferences.edit().putString("accessToken", accessToken).apply();
                                        AuthenticationActivity.this.startActivity(
                                                new Intent(AuthenticationActivity.this, DashboardActivity.class)
                                        );
                                        AuthenticationActivity.this.finish();
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    // Stopping the WebView
                    return true;
                }
                else {
                    // Returning false will make the WebView continue it's work
                    return false;
                }
            }
        });
        authenticationWebView.loadUrl(
                "https://gitter.im/login/oauth/authorize?client_id="+OauthKeys.clientID+
                "&response_type=code&redirect_uri="+OauthKeys.redirectUri
        );
    }
}
