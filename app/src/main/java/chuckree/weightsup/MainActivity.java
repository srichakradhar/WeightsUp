package chuckree.weightsup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import chuckree.weightsup.data.WeightLogContract;
import chuckree.weightsup.data.WeightLogDbHelper;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.toString();
    public static final double KGS_TO_LBS = 2.20462;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 4664;
    private TextView weight;
    private TextView target_weight;
    private TextView target_date;
    private TextView loss_gain;
    private TextView days_left;
    private TextView name_text_view;
    private ImageView user_icon;
    public static SharedPreferences preferences;
    private SQLiteDatabase mDb;
    private boolean kgs = true;
    private TextView weight_difference;
    public static final double LBS_TO_KGS = 0.453592;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weight = (TextView) findViewById(R.id.weight);
        target_weight = (TextView) findViewById(R.id.target_weight);
        target_date = (TextView) findViewById(R.id.target_date);
        loss_gain = (TextView) findViewById(R.id.loss_gain);
        days_left = (TextView) findViewById(R.id.days_left);
        name_text_view = (TextView) findViewById(R.id.name_text_view);
        user_icon = (ImageView) findViewById(R.id.user_icon);
        FloatingActionButton add_log_fab = (FloatingActionButton) findViewById(R.id.fab);
        weight_difference = (TextView) findViewById(R.id.weight_difference);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Create a DB helper (this will create the DB if run for the first time)
        WeightLogDbHelper dbHelper = new WeightLogDbHelper(this);
        // Keep a reference to the mDb until paused or killed.
        mDb = dbHelper.getWritableDatabase();


        if (preferences.getString(DetailsActivity.WEIGHT, "").length() == 0) {
            startActivity(new Intent(this, DetailsActivity.class));
        }

        add_log_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddLogActivity.class));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_fitness, menu);
        menu.findItem(R.id.action_profile).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_profile:
                Intent homeIntent = new Intent(this, MainActivity.class);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                return true;

            case R.id.details_menu_item:
                startActivity(new Intent(this, DetailsActivity.class));
                return true;

            case R.id.history_menu_item:
                startActivity(new Intent(this, WeightLogActivity.class));
                return true;

            case R.id.entry_menu_item:
                startActivity(new Intent(this, AddLogActivity.class));
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        kgs = preferences.getBoolean(DetailsActivity.UNITS, true);
        name_text_view.setText(preferences.getString(DetailsActivity.NAME, ""));

        double target_weight_val = 0;
        double weight_val = 0;

        if (preferences.contains(DetailsActivity.WEIGHT)) {
            target_weight_val = Double.parseDouble(preferences.getString(DetailsActivity.TARGET_WEIGHT, ""));
            weight_val = Double.parseDouble(preferences.getString(DetailsActivity.WEIGHT, ""));

            double firstWeight = preferences.contains(DetailsActivity.INITIAL_WEIGHT) ? Double.parseDouble(preferences.getString(DetailsActivity.INITIAL_WEIGHT, "")) : 0;
            double lossGain = firstWeight - weight_val;
            lossGain = kgs ? lossGain : lossGain * KGS_TO_LBS;
            String firstTime = getInstallDate();
            loss_gain.setText(String.format(Locale.US, "%.1f"+ (kgs ? "kg" : "lb") + " since " + firstTime, lossGain));
//        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date targetDate = new Date(preferences.getString(DetailsActivity.TARGET_DATE, ""));
            Date today = new Date();
            days_left.setText("" + TimeUnit.DAYS.convert(targetDate.getTime() - today.getTime(), TimeUnit.MILLISECONDS));
            target_date.setText(preferences.getString(DetailsActivity.TARGET_DATE, ""));
        }

        if(kgs){
            weight.setText(String.format(Locale.US, "%.1f", weight_val) + "kg");
            target_weight.setText(String.format(Locale.US, "%.1f", target_weight_val) + "kg");
            weight_difference.setText(String.format(Locale.US, "%.1fkgs to shred", weight_val - target_weight_val));
        } else {
            target_weight_val *= KGS_TO_LBS;
            weight_val *= KGS_TO_LBS;
            weight.setText(String.format(Locale.US, "%.1f", weight_val) + "lb");
            target_weight.setText(String.format(Locale.US, "%.1f", target_weight_val) + "lb");
            double weight_difference_val = weight_val - target_weight_val;
            weight_difference.setText(String.format(Locale.US, "%.1flbs to shred", weight_difference_val));
        }

        if (preferences.getBoolean(DetailsActivity.GENDER, true)) {
            user_icon.setImageResource(R.drawable.male);
        } else {
            user_icon.setImageResource(R.drawable.female);
        }

    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            return false;
        }

        return true;
    }

    private String getInstallDate() {
        // get app installation date

        PackageManager packageManager =  this.getPackageManager();
        long installTimeInMilliseconds; // install time is conveniently provided in milliseconds

        Date installDate = null;
        String installDateString = null;

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
            installTimeInMilliseconds = packageInfo.firstInstallTime;
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

            // Create a calendar object that will convert the date and time value in milliseconds to date.
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(installTimeInMilliseconds);
            installDateString = formatter.format(calendar.getTime());

        }
        catch (PackageManager.NameNotFoundException e) {
            // an error occurred, so display the Unix epoch
            installDate = new Date(0);
            installDateString = installDate.toString();
        }

        return installDateString;
    }
}
