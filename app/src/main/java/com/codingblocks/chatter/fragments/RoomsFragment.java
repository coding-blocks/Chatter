package com.codingblocks.chatter.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import com.codingblocks.chatter.MessagesDatabase;
import com.codingblocks.chatter.NoNetworkActivity;
import com.codingblocks.chatter.R;
import com.codingblocks.chatter.RoomsDatabase;
import com.codingblocks.chatter.SettingsActivity;
import com.codingblocks.chatter.SplashActivity;
import com.codingblocks.chatter.adapters.RoomsAdapter;
import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.models.RoomsDao;

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
    SharedPreferences sharedPreferences;
    RoomsDatabase roomdb;
    MessagesDatabase messagesDatabase;
    String filter;

    public static RoomsFragment newInstance(String filter) {

        RoomsFragment bottomSheetFragment = new RoomsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("filter", filter);
        bottomSheetFragment.setArguments(bundle);

        return bottomSheetFragment;

    }

    public RoomsFragment() {
        // Required empty public constructor
    }

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view)
    public RecyclerView recyclerView;
    @BindView(R.id.refreshlayout)
    SwipeRefreshLayout refreshLayout;


    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);
        ButterKnife.bind(this, view);
        filter = getArguments().getString("filter");
        db = RoomsDatabase.getInstance(getContext());
        dao = db.roomsDao();
        //for deleting db on SignOut
        roomdb = RoomsDatabase.getInstance(getContext());
        messagesDatabase = MessagesDatabase.getInstance(getContext());

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        displayRooms(mRooms, filter);
        setHasOptionsMenu(true);
        sharedPreferences =
                getActivity().getSharedPreferences("UserPreferences", 0);
        //this is swipe to refresh the Rooms fragment layout
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                displayRooms(mRooms, filter);
                refreshLayout.setRefreshing(false);
            }
        });

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
    public void displayRooms(List<RoomsTable> rooms, final String filter) {
        /* No rooms, let's get them first */
        if (rooms.size() == 0) {
            /* Internet is needed for sure to get the rooms */
            getRooms(1);

        }
        new AsyncTask<Void, Void, List<RoomsTable>>() {

            @Override
            protected List<RoomsTable> doInBackground(Void... voids) {
                Log.i(TAG, "doInBackground: " + filter);
                switch (filter) {
                    case "All":
                        return dao.getAllRooms();
                    case "oneToone":
                        return dao.getPeopleRooms();
                    default:
                        return dao.getAllRooms();
                }
            }

            @Override
            protected void onPostExecute(List<RoomsTable> notes) {
                mRooms.clear();
                mRooms.addAll(notes);
            }
        }.execute();

        adapter = new RoomsAdapter(rooms, getContext());
        recyclerView.setAdapter(adapter);

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
                                    boolean roomMember = dynamicJObject.getBoolean("roomMember");
                                    String topic = dynamicJObject.getString("topic");

                                    Log.i(TAG, "run: " + dynamicJObject.toString());
                                    String favourite = null;
                                    if (!dynamicJObject.isNull("favourite"))
                                        favourite = (dynamicJObject.getString("favourite"));
                                    Log.i(TAG, "run: " + favourite);

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
                                    room.setTopic(topic);
                                    room.setRoomName(name);
                                    room.setUserCount(userCount);
                                    room.setUnreadItems(unreadItems);
                                    room.setMentions(mentions);
                                    room.setRoomAvatar(url);
                                    room.setRoomMember(roomMember);
                                    room.setFavourite(favourite);
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
                            } finally {
                                if (mRooms.size() == 0) {
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_options_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                displayRooms(mRooms,filter);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mRooms.clear();
                adapter.notifyDataSetChanged();
                searchRooms(newText);
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchRooms(String newText) {
        if (isNetworkAvailable()) {
            /* Display a toast to inform the user that we are syncing */
            String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");
            if (accessToken.equals("")) {
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms?q=" + newText)
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
                    final String responseText = response.body().string();
                    // We will move to UI Thread
                    Thread thread = new Thread(new Runnable() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        public void run() {
                            try {
                                JSONObject JObject = new JSONObject(responseText);
                                Log.i(TAG, "run: " + responseText);
                                JSONArray JArray = JObject.getJSONArray("results");
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
                                    boolean roomMember = dynamicJObject.getBoolean("roomMember");
                                    Log.i(TAG, "run: " + dynamicJObject.toString());
                                    final RoomsTable room = new RoomsTable();
                                    room.setuId(uId);
                                    room.setRoomName(name);
                                    room.setUserCount(userCount);
                                    room.setUnreadItems(unreadItems);
                                    room.setMentions(mentions);
                                    room.setRoomAvatar(url);
                                    room.setRoomMember(roomMember);
                                    mRooms.add(room);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } finally {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                }
            });
            /* Prompt user to turn on internet only if we have no rooms */
        } else {
            Intent intent = new Intent(getActivity(), NoNetworkActivity.class);
            intent.putExtra("calledFrom", "DashboardActivity");
            getActivity().startActivity(intent);
            getActivity().finish();
        }
    }

}
