package com.codingblocks.chatter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.displayName)
    TextView displaynametv;
    @BindView(R.id.userName)
    TextView userNametv;
    @BindView(R.id.repoTextView)
    TextView repotv;
    @BindView(R.id.followersTextView)
    TextView followerstv;
    @BindView(R.id.followingTextView)
    TextView followingtv;
    @BindView(R.id.locationTextView)
    TextView locationtv;
    @BindView(R.id.emailbtn)
    Button emailbtn;
    @BindView(R.id.websitebtn)
    Button websitebtn;
    @BindView(R.id.profilebtn)
    Button profilebtn;


    private OkHttpClient client = new OkHttpClient();
    private String userId;
    private String userName;
    String email;
    String website;
    String profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        ButterKnife.bind(this);
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        userId = (String) bundle.get("userId");
        userName = (String) bundle.get("userName");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle(userName);
        String accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/"
                        + "users/" +
                        userName)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    Log.i("TAG", "onResponse: " + jsonObject.toString());
                    final String username = jsonObject.getString("username");
                    final String displayName = jsonObject.getString("displayName");
                    final String location = jsonObject.getString("location");
                    email = jsonObject.getString("email");
                    website = jsonObject.getString("website");
                    profile = jsonObject.getString("profile");
                    String company = jsonObject.getString("company");
                    JSONObject github = jsonObject.getJSONObject("github");
                    final String repos = github.getString("public_repos");
                    final String followers = github.getString("followers");
                    final String following = github.getString("following");
                    Log.i("TAG", "UseronResponse: " + username + displayName + location + email + website + profile + company + github);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displaynametv.setText(displayName);
                            userNametv.setText("@" + username);
                            repotv.setText(repos);
                            followerstv.setText(followers);
                            followingtv.setText(following);
                            locationtv.setText(location);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    profilebtn.setOnClickListener(UserActivity.this);
                    websitebtn.setOnClickListener(UserActivity.this);
                    emailbtn.setOnClickListener(UserActivity.this);
                }

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent i;
        Uri a;
        switch (id) {
            case R.id.emailbtn:
                i = new Intent(Intent.ACTION_SENDTO);
                a = Uri.parse("mailto:" + email);
                i.setData(a);
                startActivity(i);
                break;
            case R.id.websitebtn:
                i = new Intent(Intent.ACTION_VIEW);
                a = Uri.parse(website);
                i.setData(a);
                startActivity(i);
                break;
            case R.id.profilebtn:
                i = new Intent(Intent.ACTION_VIEW);
                a = Uri.parse(profile);
                i.setData(a);
                startActivity(i);
                break;

        }


    }
}
