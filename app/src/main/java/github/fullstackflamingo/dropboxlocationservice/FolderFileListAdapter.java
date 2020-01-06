package github.fullstackflamingo.dropboxlocationservice;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class FolderFileListAdapter extends RecyclerView.Adapter<FolderFileListAdapter.MyViewHolder> {
    private ListItem[] dataset;
    private Callback listItemCallback;
    private Context ctx;

    public interface Callback {
        void onClick(String newPath);
        void onClick(FileMetadata md);
    }

    public static class ListItem {
        JSONObject jo = null;
        Metadata md = null;
        Boolean isLocation = false;

        public ListItem(Metadata md) {
            this.md = md;
        }

        public ListItem(JSONObject jo) {
            this.jo = jo;
            this.isLocation = true;
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public View itemView;
        public TextView textView;
        public ImageView iconView;

        public MyViewHolder(View item) {
            super(item);
            itemView = item;
            textView = item.findViewById(R.id.folder_file_list_item_text);
            iconView = item.findViewById(R.id.folder_file_list_item_icon);
        }
    }

    public FolderFileListAdapter(Context ctx, Callback listItemCallback) {
        this.listItemCallback = listItemCallback;
        this.ctx = ctx;
    }

    public void updateDataset(ListItem[] result) {
        dataset = result;
        notifyDataSetChanged();
    }

    @Override
    public FolderFileListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folderfilelistitem, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        final ListItem li = dataset[position];
        holder.iconView.setImageResource(R.drawable.ic_map_pin);
        if (li.isLocation) {
            try {
                String prettyDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date(li.jo.optLong("date_ms")));
                holder.textView.setText(
                        prettyDate +
                                "\nProvider: " + li.jo.optString("provider") +
                            "\nTime taken: " + li.jo.optLong("location_acquisition_time_ms") + "ms" );
                                //"\nlat: " + li.jo.getDouble("lat") + ", lon: " + li.jo.getDouble("lon"));
                final Double lat = li.jo.optDouble("lat");
                final Double lon = li.jo.optDouble("lon");
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW);
//                          String url = "http://maps.google.com/maps?z=12&t=m&q=loc:%s+%s";
                            // https://developers.google.com/maps/documentation/urls/guide#search-examples
                            String url = "https://www.google.com/maps/search/?api=1&query=%s,%s";
                            i.setData(Uri.parse(String.format(url, lat, lon)));
                            ctx.startActivity(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                holder.textView.setText("couldn't get name");
            }

        } else {
            holder.textView.setText(li.md.getName().replace(".json", ""));

            if (li.md instanceof FileMetadata) {
                holder.iconView.setImageResource(R.drawable.ic_calendar_today_24px);
            }
            else {
                holder.iconView.setImageResource(R.drawable.ic_folder_24px);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (li.md instanceof FileMetadata) {
                        listItemCallback.onClick((FileMetadata) li.md);
                    } else {
                        listItemCallback.onClick(li.md.getPathDisplay());
                    }
                }
            });

        }
    }

    @Override
    public void onViewRecycled(@NonNull MyViewHolder holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return dataset != null ? dataset.length : 0;
    }
}
