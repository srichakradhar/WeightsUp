package chuckree.weightsup;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import chuckree.weightsup.data.WeightLogContract;
import chuckree.weightsup.data.WeightLogDbHelper;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static chuckree.weightsup.DetailsActivity.DATE_DIALOG_ID;
import static chuckree.weightsup.DetailsActivity.WEIGHT;
import static chuckree.weightsup.R.id.target_date_edit_text;

public class AddLogActivity extends AppCompatActivity {

    private static final String TAG = AddLogActivity.class.toString();
    private SQLiteDatabase mDb;
    private boolean kgs = true;
    private int year;
    private int month;
    private int day;
    private EditText current_date_edit_text;

    Bitmap progress_pic_bitmap;
    Uri progress_pic_uri;


    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();

    private final static int ALL_PERMISSIONS_RESULT = 107;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.add_log);
        setContentView(R.layout.activity_add_log);

        final EditText weight_log_edit_text = (EditText) findViewById(R.id.weight_log_edit_text);
        current_date_edit_text = (EditText) findViewById(R.id.current_date_edit_text);
        Button add_log_button = (Button) findViewById(R.id.button_add_log);
        FloatingActionButton pic_fab = (FloatingActionButton) findViewById(R.id.pic_fab);

        // Create a DB helper (this will create the DB if run for the first time)
        WeightLogDbHelper dbHelper = new WeightLogDbHelper(this);

        // Keep a reference to the mDb until paused or killed.
        mDb = dbHelper.getWritableDatabase();
        kgs = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(DetailsActivity.UNITS, true);

        current_date_edit_text.setText(R.string.today);

        pic_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivityForResult(getPickImageChooserIntent(), 200);

            }
        });

        current_date_edit_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        add_log_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    String weight = weight_log_edit_text.getText().toString().trim();
                    addNewLog(Float.parseFloat(weight));
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    editor.putString(WEIGHT, kgs ? weight : String.valueOf(Double.parseDouble(weight) * MainActivity.LBS_TO_KGS));
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "Successfully logged the weight", Toast.LENGTH_SHORT).show();
                    finish();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Please enter a valid weight", Toast.LENGTH_LONG).show();
                }
            }
        });

        permissions.add(CAMERA);
        permissions.add(READ_EXTERNAL_STORAGE);
        permissions.add(WRITE_EXTERNAL_STORAGE);
        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }

        final Button units_popup_button = (Button) findViewById(R.id.units_popup_button);
        units_popup_button.setText(kgs ? "kgs" : "lbs");

        units_popup_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(AddLogActivity.this, units_popup_button);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.menu_units_popup, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()){

                            case R.id.kgs_popup:
                                kgs = true;
                                units_popup_button.setText("kgs");
                                break;

                            case R.id.lbs_popup:
                                kgs = false;
                                units_popup_button.setText("lbs");
                                break;
                        }

                        return true;
                    }
                });

                popup.show(); //showing popup menu
            }
        }); //closing the setOnClickListener method

    }

    private long addNewLog(Float weight) {
        ContentValues cv = new ContentValues();
        cv.put(WeightLogContract.WeightLogEntry.COLUMN_WEIGHT, kgs ? weight : weight * MainActivity.LBS_TO_KGS);
        Cursor mC = mDb.rawQuery("SELECT weight FROM weightlog ORDER BY timestamp DESC LIMIT 1", null);
        Float latestWeight = 0f;
        try {
            mC.moveToPosition(0);
            latestWeight = Float.parseFloat(mC.getString(0));
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        } finally {
            mC.close();
        }
        String today = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(new Date());
        String user_date = current_date_edit_text.getText().toString();
        if(!today.equals(user_date) && !user_date.equals(getString(R.string.today))){
            cv.put(WeightLogContract.WeightLogEntry.COLUMN_TIMESTAMP, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(user_date)));
        }
        if (progress_pic_uri != null)
            cv.put(WeightLogContract.WeightLogEntry.COLUMN_IMAGE_PATH, progress_pic_uri.toString());
        return mDb.insert(WeightLogContract.WeightLogEntry.TABLE_NAME, null, cv);
    }

    public Intent getPickImageChooserIntent() {

        // Determine Uri of camera image to save.
        Uri outputFileUri = getCaptureImageOutputUri();

        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }

        // collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // the main intent is the last in the list. so pickup the useless one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals("com.android.documentsui.DocumentsActivity")) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, "Select source");

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }


    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "progress_pic.jpg"));
        }
        return outputFileUri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {

            ImageView progress_picture = (ImageView) findViewById(R.id.progress_picture);

            if (getPickImageResultUri(data) != null) {
                progress_pic_uri = getPickImageResultUri(data);

                try {
                    progress_pic_bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), progress_pic_uri);
                    if (data != null && data.getScheme().equals("content")) {
                        InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(progress_pic_uri);
                        progress_pic_bitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();
                    } else {
                        progress_pic_bitmap = rotateImageIfRequired(progress_pic_bitmap, progress_pic_uri);
                    }

                    progress_pic_bitmap = getResizedBitmap(progress_pic_bitmap, 500);

                    progress_picture.setImageBitmap(progress_pic_bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {

                progress_pic_bitmap = (Bitmap) data.getExtras().get("data");
                progress_picture.setImageBitmap(progress_pic_bitmap);

            }

        }

    }

    private static Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {

        ExifInterface ei = new ExifInterface(selectedImage.getPath());
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }


    /**
     * Get the URI of the selected image from {@link #getPickImageChooserIntent()}.
     * <p>
     * Will return the correct URI for camera and gallery image.
     *
     * @param data the returned data of the activity result
     */
    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }


        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on scren orientation
        // changes
        outState.putParcelable("progress_pic_uri", progress_pic_uri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        progress_pic_uri = savedInstanceState.getParcelable("progress_pic_uri");
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DATE_DIALOG_ID:
                // set date picker as current date
                final Calendar c = Calendar.getInstance();
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);

                return new DatePickerDialog(this, datePickerListener,
                        year, month, day);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener datePickerListener
            = new DatePickerDialog.OnDateSetListener() {

        // when dialog box is closed, below method will be called.
        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {
            year = selectedYear;
            month = selectedMonth;
            day = selectedDay;

            // set selected date into EditText
            current_date_edit_text.setText(new StringBuilder().append(month + 1)
                    .append("/").append(day).append("/").append(year)
                    .append(" "));

            // set selected date into datepicker also
//            date_picker.init(year, month, day, null);

        }
    };

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (hasPermission(perms)) {

                    } else {

                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }
}
