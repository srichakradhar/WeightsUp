package chuckree.weightsup;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import chuckree.weightsup.data.WeightLogContract;
import chuckree.weightsup.data.WeightLogDbHelper;

public class WeightLogActivity extends AppCompatActivity {

    private SQLiteDatabase mDb;
    private WeightLogAdapter mAdapter;
    RecyclerView weightLogRecyclerView;
    boolean kgs = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.weight_log);
        setContentView(R.layout.activity_weight_log);

        FloatingActionButton add_log_fab = (FloatingActionButton) findViewById(R.id.weight_log_fab);

        // Set local attributes to corresponding views
        weightLogRecyclerView = (RecyclerView) this.findViewById(R.id.weight_log_list_view);

        // Set layout for the RecyclerView, because it's a list we are using the linear layout
        weightLogRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a DB helper (this will create the DB if run for the first time)
        WeightLogDbHelper dbHelper = new WeightLogDbHelper(this);

        // Keep a reference to the mDb until paused or killed.
        mDb = dbHelper.getWritableDatabase();

        // Get all logs from the database and save in a cursor
        Cursor cursor = getAllLogs();

        // Create an adapter for that cursor to display the data
        mAdapter = new WeightLogAdapter(this, cursor);

        // Link the adapter to the RecyclerView
        weightLogRecyclerView.setAdapter(mAdapter);

        add_log_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WeightLogActivity.this, AddLogActivity.class));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Cursor cursor = getAllLogs();
        mAdapter = new WeightLogAdapter(this, cursor);
        weightLogRecyclerView.setAdapter(mAdapter);
    }

    class WeightLogAdapter extends RecyclerView.Adapter<WeightLogAdapter.WeightLogViewHolder> {

        // Holds on to the cursor to display the waitlist
        private Cursor mCursor;
        private Context mContext;

        public WeightLogAdapter(Context context, Cursor cursor){
            this.mContext = context;
            this.mCursor = cursor;
        }

        @Override
        public WeightLogAdapter.WeightLogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Get the RecyclerView item layout
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.log_item, parent, false);
            return new WeightLogViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final WeightLogAdapter.WeightLogViewHolder holder, final int position) {
            // Move the mCursor to the position of the item to be displayed
            if (!mCursor.moveToPosition(position))
                return; // bail if returned null

            kgs = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(DetailsActivity.UNITS, true);
            // Update the view holder with the information needed to display
            double weight = mCursor.getFloat(mCursor.getColumnIndex(WeightLogContract.WeightLogEntry.COLUMN_WEIGHT));
            String date = mCursor.getString(mCursor.getColumnIndex(WeightLogContract.WeightLogEntry.COLUMN_TIMESTAMP));
            long id = mCursor.getLong(mCursor.getColumnIndex(WeightLogContract.WeightLogEntry._ID));
            double lossGain = mCursor.getFloat(mCursor.getColumnIndex(WeightLogContract.WeightLogEntry.COLUMN_LOSS_GAIN));
            String image_path = mCursor.getString(mCursor.getColumnIndex(WeightLogContract.WeightLogEntry.COLUMN_IMAGE_PATH));

            if(!kgs){
                weight *= MainActivity.KGS_TO_LBS;
                lossGain *= MainActivity.KGS_TO_LBS;
            }

            holder.weightTextView.setText(String.format(Locale.US, "Weight: %.1f" + (kgs ? "kg" : "lb"), weight));
            holder.dateTextView.setText(String.format(getResources().getString(R.string.dynamic_date), date));
            holder.deleteLogButton.setTag(id);
            holder.lossGainTextView.setText(String.format(Locale.US, (lossGain < 0 ? "Gain" : "Loss") + ": %.1f" + (kgs ? "kg" : "lb"), Math.abs(lossGain)));
            if(image_path != null){
                Bitmap progressPicBitmap;
                if(image_path.startsWith("content:")){
                    InputStream inputStream = null;
                    try {
                        inputStream = getApplicationContext().getContentResolver().openInputStream(Uri.parse(image_path));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    progressPicBitmap = BitmapFactory.decodeStream(inputStream);
                    if( inputStream != null ){
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    progressPicBitmap = BitmapFactory.decodeFile(image_path.replace("file:",""));
                }
                holder.progressPicImageView.setImageBitmap(progressPicBitmap);

            }
            holder.deleteLogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    long id = (long) v.getTag();
                    //remove from DB
                    removeLog(id);
                    //update the list
                    mAdapter.swapCursor(getAllLogs());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }

        public void swapCursor(Cursor newCursor) {
            // Always close the previous mCursor first
            if (mCursor != null) mCursor.close();
            mCursor = newCursor;
            if (newCursor != null) {
                // Force the RecyclerView to refresh
                this.notifyDataSetChanged();
            }
        }

        class WeightLogViewHolder extends RecyclerView.ViewHolder {

            TextView dateTextView;
            TextView weightTextView;
            TextView lossGainTextView;
            Button deleteLogButton;
            ImageView progressPicImageView;

            public WeightLogViewHolder(View itemView) {
                super(itemView);
                dateTextView = (TextView) itemView.findViewById(R.id.log_item_date);
                weightTextView = (TextView) itemView.findViewById(R.id.log_item_weight);
                lossGainTextView = (TextView) itemView.findViewById(R.id.log_item_loss_gain);
                deleteLogButton = (Button) itemView.findViewById(R.id.button_delete);
                progressPicImageView = (ImageView) itemView.findViewById(R.id.progress_picture_log);
            }
        }
    }

    private boolean removeLog(long id) {
        // COMPLETED (2) Inside, call mDb.delete to pass in the TABLE_NAME and the condition that WaitlistEntry._ID equals id
        return mDb.delete(WeightLogContract.WeightLogEntry.TABLE_NAME, WeightLogContract.WeightLogEntry._ID + "=" + id, null) > 0;
    }

    private Cursor getAllLogs() {
        return mDb.query(
                WeightLogContract.WeightLogEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                WeightLogContract.WeightLogEntry.COLUMN_TIMESTAMP
        );
    }
}
