package com.codingblocks.chatter.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.codingblocks.chatter.NoNetworkActivity;
import com.codingblocks.chatter.R;
import com.codingblocks.chatter.models.RoomsDao;
import com.codingblocks.chatter.RoomsDatabase;
import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.SplashActivity;
import com.codingblocks.chatter.adapters.RoomsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RoomsFragment extends Fragment {

    private static final String TAG = "TestingGitter";
    private RoomsAdapter adapter;
    List<RoomsTable> mRooms = new ArrayList<>();
    RoomsDatabase db;
    RoomsDao dao;


    public RoomsFragment() {
        // Required empty public constructor
    }

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view)
    public RecyclerView recyclerView;

    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);
        ButterKnife.bind(this, view);

        db = RoomsDatabase.getInstance(getContext());
        dao = db.roomsDao();

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        displayRooms(mRooms);

        return view;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @SuppressLint("StaticFieldLeak")
    public void displayRooms(List<RoomsTable> rooms) {
        /* No rooms, let's get them first */
        if (rooms.size() == 0) {
            /* Internet is needed for sure to get the rooms */
            getRooms(1);

        }
        new AsyncTask<Void, Void, List<RoomsTable>>() {

            @Override
            protected List<RoomsTable> doInBackground(Void... voids) {
                return dao.getAllRooms();
            }

            @Override
            protected void onPostExecute(List<RoomsTable> notes) {
                mRooms.clear();
                mRooms.addAll(notes);
            }
        }.execute();

        adapter = new RoomsAdapter(rooms, getContext());
        recyclerView.setAdapter(adapter);
        new AsyncTask<Void, Void, List<RoomsTable>>() {

            @Override
            protected List<RoomsTable> doInBackground(Void... voids) {
                return dao.getAllRooms();
            }

            @Override
            protected void onPostExecute(List<RoomsTable> notes) {
                mRooms.clear();
                mRooms.addAll(notes);
            }
        }.execute();

        /* Get rooms if network is available
           [we have old ones but checking for updates] */
        getRooms(0);
    }

    public void getRooms(int severity) {
        if (isNetworkAvailable()) {
            /* Display a toast to inform the user that we are syncing */
            Toast.makeText(
                    getActivity(), "Syncing data", Toast.LENGTH_SHORT
            ).show();
            String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");

            if (accessToken.equals("")) {
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms")
                    .build();
            HttpUrl url = request.url().newBuilder()
                    .addQueryParameter("access_token", accessToken)
                    .build();
            request = request.newBuilder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    /* Simple hack for compatibility as API 19 is required for
                       new JSONArray */
                    final String responseText = "{\"rooms\":" + response.body().string() + "}";
                    // We will move to UI Thread
                    Thread thread = new Thread(new Runnable() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        public void run() {
                            try {
                                JSONObject JObject = new JSONObject(responseText);
                                JSONArray JArray = JObject.getJSONArray("rooms");
                                int i;
                                for (i = 0; i < JArray.length(); i++) {

                                    JSONObject dynamicJObject = JArray.getJSONObject(i);
                                    String githubType = dynamicJObject.getString("githubType");
                                    final String uId = dynamicJObject.getString("id");
                                    String name = dynamicJObject.getString("name");
                                    String url = dynamicJObject.getString("avatarUrl");
                                    // userCount = 0 == user to user room since ONETWOONE doesnot have it
                                    int userCount = 0;
                                    if (!githubType.equals("ONETWOONE")) {
                                        userCount = dynamicJObject.getInt("userCount");
                                    }
                                    int unreadItems = dynamicJObject.getInt("unreadItems");
                                    int mentions = dynamicJObject.getInt("mentions");
                                    Log.i(TAG, "run: " + dynamicJObject.toString());

//


//
//                                    // Get the current max id in the EntityName table
                                    int maxId = dao.getMax();
                                    Log.i(TAG, "onPostExecute: dao max" + dao.getMax());
                                    // If id is null, set it to 1, else set increment it by 1
                                    int nextId = (maxId == 0) ? 1 : maxId + 1;

                                    RoomsTable containedRoom = dao.getRoomWithuId(uId);
                                    if (containedRoom != null) {
                                        // Save the id so that if when we delete, we can insert it into the empty slot
                                        // since we are sorting by id
                                        nextId = containedRoom.getId();
                                        // Delete that room, so you can push an update ;)
                                        dao.delete(containedRoom);
                                        mRooms.remove(containedRoom);
                                    }

                                    final RoomsTable room = new RoomsTable();
                                    room.setId(nextId);
                                    room.setuId(uId);
                                    room.setRoomName(name);
                                    room.setUserCount(userCount);
                                    room.setUnreadItems(unreadItems);
                                    room.setMentions(mentions);
                                    room.setRoomAvatar(url);
//
//                                    // Begin, copy and commit
////                                    realm.beginTransaction();
////                                    realm.copyToRealmOrUpdate(room);
////                                    realm.commitTransaction();
                                    new AsyncTask<Void, Void, Void>() {

                                        @Override
                                        protected Void doInBackground(Void... voids) {
                                            try {
                                                dao.addRooms(room);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            return null;
                                        }

                                        @Override
                                        protected void onPostExecute(Void aVoid) {
                                            super.onPostExecute(aVoid);
                                            adapter.notifyDataSetChanged();

                                        }
                                    }.execute();
                                }

                                if (i == 0) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(
                                                    getActivity(),
                                                    "There seems to be no rooms, please try again later",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }finally {
                                if(mRooms.size()== 0){
                                    new AsyncTask<Void, Void, List<RoomsTable>>() {

                                        @Override
                                        protected List<RoomsTable> doInBackground(Void... voids) {
                                            return dao.getAllRooms();
                                        }

                                        @Override
                                        protected void onPostExecute(List<RoomsTable> notes) {
                                            mRooms.clear();
                                            mRooms.addAll(notes);
                                        }
                                    }.execute();
                                }
                            }
                        }
                    });
                    thread.start();
                }
            });
            /* Prompt user to turn on internet only if we have no rooms */
        } else if (severity == 1) {
            Intent intent = new Intent(getActivity(), NoNetworkActivity.class);
            intent.putExtra("calledFrom", "DashboardActivity");
            getActivity().startActivity(intent);
            getActivity().finish();
        }
    }
}
