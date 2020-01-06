package github.fullstackflamingo.dropboxlocationservice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_INT = 54254;
    private static final String TAG = "MainActivity";
    private ViewGroup loadingSpinner;
    private RecyclerView recyclerView;
    private FolderFileListAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private ActionBar actionBar;

    private Boolean loading = false;

    private String currentPath = "";
    private FileMetadata virtualJSONFolderFileMetaData = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainlayout);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        loadingSpinner = findViewById(R.id.folder_file_list_item_spinner);

        recyclerView = findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayout.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        // specify an adapter (see also next example)
        mAdapter = new FolderFileListAdapter(this, new FolderFileListAdapter.Callback() {
            @Override
            public void onClick(String newPath) {
                if (loading) return;
                getDBxList(newPath);
            }

            @Override
            public void onClick(FileMetadata md) {
                if (loading) return;
                getDBxListFromJSONFile(md);
            }
        });
        recyclerView.setAdapter(mAdapter);

        getDBxList();

        Context context = getApplicationContext();
        if (
                context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_INT);
            return;
        } else {
            LocationUploadWorker.enqueueSelf(context);
        }
    }
    private void getDBxList() {
        getDBxList(currentPath);
    }

    @NonNull
    private void getDBxList(String newPath) {
        if (loading) return;
        final String pathToLoad = newPath != null ? newPath : currentPath;
        loading = true;
        loadingSpinner.setVisibility(View.VISIBLE);
        try {
            new DropboxListFolderTask(this,new DropboxListFolderTask.Callback() {
                @Override
                public void onComplete(ListFolderResult result) {
                    loading = false;
                    loadingSpinner.setVisibility(View.INVISIBLE);
                    currentPath = pathToLoad;
                    virtualJSONFolderFileMetaData = null;
                    actionBar.setDisplayHomeAsUpEnabled(currentPath != "");
                    // results list to ListItem array
                    List<Metadata> list = result.getEntries();
                    FolderFileListAdapter.ListItem[] datasetArray = new FolderFileListAdapter.ListItem[list.size()];
                    //for(int i = list.size() - 1; i >= 0; i--) {
                    for(int i = 0; i < list.size(); i+= 1) {
                        datasetArray[list.size() -1 - i] = new FolderFileListAdapter.ListItem(list.get(i));
                    }
                    mAdapter.updateDataset(datasetArray);
                }

                @Override
                public void onError(Exception e) {
                    loading = false;
                    loadingSpinner.setVisibility(View.INVISIBLE);
                    e.printStackTrace();

                }
            }).execute(pathToLoad);
        } catch (Exception e) {
            e.printStackTrace();
            loading = false;
            loadingSpinner.setVisibility(View.INVISIBLE);
        }
    }
    @NonNull
    private void getDBxListFromJSONFile(final FileMetadata md) {
        if (!md.getPathLower().contains(".json")) return;
        if (loading) return;
        loading = true;
        loadingSpinner.setVisibility(View.VISIBLE);
        try {
            new DropboxDownloadJSONFileTask(this,new DropboxDownloadJSONFileTask.Callback() {
                @Override
                public void onComplete(JSONArray result) {
                    loading = false;
                    loadingSpinner.setVisibility(View.INVISIBLE);
                    actionBar.setDisplayHomeAsUpEnabled(true);
                    virtualJSONFolderFileMetaData = md;

                    FolderFileListAdapter.ListItem[] datasetArray = new FolderFileListAdapter.ListItem[result.length()];
                    for(int i = result.length() - 1; i >= 0; i--) {
                    // for(int i = 0; i < result.length(); i+= 1) {
                        try {
                            Object o = result.get(i);
                            datasetArray[result.length() -1 - i] = new FolderFileListAdapter.ListItem((JSONObject) o);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    mAdapter.updateDataset(datasetArray);
                }

                @Override
                public void onError(Exception e) {
                    loading = false;
                    loadingSpinner.setVisibility(View.INVISIBLE);
                    e.printStackTrace();

                }
            }).execute(md);
        } catch (Exception e) {
            loading = false;
            loadingSpinner.setVisibility(View.INVISIBLE);
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_INT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationUploadWorker.enqueueSelf(this);
                }
                return;
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (currentPath == "") {
            super.onBackPressed();
            return;
        }
        if (virtualJSONFolderFileMetaData == null) {
            currentPath = "";
        } else {
            virtualJSONFolderFileMetaData = null;
        }
        getDBxList();
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (virtualJSONFolderFileMetaData == null) {
                    getDBxList();
                } else {
                    getDBxListFromJSONFile(virtualJSONFolderFileMetaData);
                }
                return true;
            /*case R.id.settings:
                // TODO: Open dialog to set encrypted Dropbox access token via AppPreferences
                // https://developer.android.com/guide/topics/ui/dialogs#CustomLayout
                return true;*/
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


}