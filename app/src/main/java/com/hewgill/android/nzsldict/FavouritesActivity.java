package com.hewgill.android.nzsldict;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class FavouritesActivity extends BaseActivity {
    private ListView mListView;
    private DictionaryAdapter adapter;
    private FavouritesRepository repo;
    private DownloadReceiver mDownloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Favourites");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_favourites);
        registerDownloadReceiver();
        mListView = (ListView) findViewById(R.id.favourites);
        repo = new FavouritesRepository(this);
        adapter = new DictionaryAdapter(this, R.layout.list_item, repo.all());
        getListView().setAdapter(adapter);
        mListView.setEmptyView(findViewById(R.id.empty_favourites));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick((ListView) parent, view, position, id);
            }
        });

        findViewById(R.id.finish_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterDownloadReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.favourites_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        DictItem item = (DictItem) getListView().getItemAtPosition(position);
        Log.d("list", item.gloss);
        Intent next = new Intent();
        next.setClass(this, WordActivity.class);
        next.putExtra("item", item);
        startActivity(next);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favourites_clear:
                clearFavourites();
                break;
            case R.id.action_favourites_download:
                enqueueFavouritesDownload();
                break;
        }

        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    private void clearFavourites() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to clear your favourites?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        for (DictItem item : repo.all()) {
                            repo.remove(item);
                        }
                        ((DictionaryAdapter) getListView().getAdapter()).setWords(new ArrayList<DictItem>());
                        ((DictionaryAdapter) getListView().getAdapter()).notifyDataSetChanged();
                    }})
                .setNegativeButton(android.R.string.no, null).show();

    }

    private void enqueueFavouritesDownload() {
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        for (DictItem item : repo.all()) {
            if (new DictItemOfflineAvailability(this, item).availableOffline()) {
                Log.d(getLocalClassName(), item.video + " was already downloaded.");
                continue;
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.video));
            Log.d(getLocalClassName(), "Enqueuing download for " + item.video);
            request.setDestinationInExternalFilesDir(this, "videos", item.videoFilename());
            request.setTitle("Sign video: " + item.gloss);
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            downloadManager.enqueue(request);
        }

        Toast.makeText(this, "Downloading favourites...", Toast.LENGTH_LONG).show();
    }

    private void registerDownloadReceiver() {
        mDownloadReceiver = new DownloadReceiver();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        this.registerReceiver(mDownloadReceiver, filter);
    }

    private void unregisterDownloadReceiver() {
        this.unregisterReceiver(mDownloadReceiver);
    }

    private ListView getListView() { return mListView; }
}