package chuckree.weightsup;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class DetailsActivity extends AppCompatActivity {

    public static final String WEIGHT = "WEIGHT";
    public static final String TARGET_DATE = "TARGET_DATE";
    public static final String TARGET_WEIGHT = "TARGET_WEIGHT";
    public static final String GENDER = "GENDER";
    public static final String NAME = "NAME";
    public static final String UNITS = "UNITS";
    public static final String INITIAL_WEIGHT = "INITIAL_WEIGHT";
    public static SharedPreferences preferences;
    private boolean gender = true;
    private boolean kgs = true;
    private EditText name_edit_text;
    private EditText weight_edit_text;
    private EditText target_weight_edit_text;
    private EditText target_date_edit_text;
    private RadioButton male_option;
    private RadioButton female_option;
    private RadioButton kgs_option;
    private RadioButton lbs_option;
    private DatePicker date_picker;
    static final int DATE_DIALOG_ID = 999;

    private int year;
    private int month;
    private int day;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.details);
        setContentView(R.layout.activity_details);

        name_edit_text = (EditText) findViewById(R.id.name_edit_text);
        weight_edit_text = (EditText) findViewById(R.id.weight_edit_text);
        target_weight_edit_text = (EditText) findViewById(R.id.target_weight_edit_text);
        target_date_edit_text = (EditText) findViewById(R.id.target_date_edit_text);
        Button save_button = (Button) findViewById(R.id.save_button);
        male_option = (RadioButton) findViewById(R.id.male_option);
        female_option = (RadioButton) findViewById(R.id.female_option);
        RadioGroup gender_radio_group = (RadioGroup) findViewById(R.id.gender_radio_group);
        RadioGroup units_radio_group = (RadioGroup) findViewById(R.id.units_radio_group);
        kgs_option = (RadioButton) findViewById(R.id.kgs_option);
        lbs_option = (RadioButton) findViewById(R.id.lbs_option);
//        date_picker=(DatePicker)findViewById(R.id.date_picker);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        kgs = preferences.getBoolean(UNITS, true);

        gender_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                gender = (checkedId == R.id.male_option);
            }
        });

        units_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                kgs = (checkedId == R.id.kgs_option);
                double weight_val = Double.parseDouble(weight_edit_text.getText().toString());
                double target_weight_val = Double.parseDouble(target_weight_edit_text.getText().toString());
                if(kgs){
                    weight_edit_text.setText(String.format(Locale.US, "%.1f", weight_val * MainActivity.LBS_TO_KGS));
                    target_weight_edit_text.setText(String.format(Locale.US, "%.1f", target_weight_val * MainActivity.LBS_TO_KGS));
                }else{
                    weight_edit_text.setText(String.format(Locale.US, "%.1f", weight_val * MainActivity.KGS_TO_LBS));
                    target_weight_edit_text.setText(String.format(Locale.US, "%.1f", target_weight_val * MainActivity.KGS_TO_LBS));
                }
            }
        });

        target_date_edit_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        if (!preferences.contains(GENDER)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(GENDER, true);
            editor.putBoolean(UNITS, true);
            editor.apply();
        }


        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (name_edit_text.getText().toString().trim().length() > 0 &&
                        weight_edit_text.getText().toString().trim().length() > 0 &&
                        target_weight_edit_text.getText().toString().trim().length() > 0 &&
                        target_date_edit_text.getText().toString().trim().length() > 0) {

                    SharedPreferences.Editor editor = preferences.edit();

                    double weight_val = Double.parseDouble(weight_edit_text.getText().toString());
                    double target_weight_val = Double.parseDouble(target_weight_edit_text.getText().toString());
                    weight_val = kgs ? weight_val :  weight_val * MainActivity.LBS_TO_KGS;
                    target_weight_val = kgs ? target_weight_val :  target_weight_val * MainActivity.LBS_TO_KGS;

                    editor.putString(WEIGHT,String.format(Locale.US, "%.1f", weight_val));
                    editor.putString(TARGET_WEIGHT, String.format(Locale.US, "%.1f", target_weight_val));

                    if(!preferences.contains(INITIAL_WEIGHT))
                        editor.putString(INITIAL_WEIGHT, String.format(Locale.US, "%.1f", weight_val));
                    editor.putString(TARGET_DATE, target_date_edit_text.getText().toString().trim());
                    editor.putBoolean(GENDER, gender);
                    editor.putString(NAME, name_edit_text.getText().toString().trim());
                    editor.putBoolean(UNITS, kgs);
                    editor.apply();

                    Toast.makeText(getApplicationContext(), "Your details have been saved", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
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
            target_date_edit_text.setText(new StringBuilder().append(month + 1)
                    .append("/").append(day).append("/").append(year)
                    .append(" "));

            // set selected date into datepicker also
//            date_picker.init(year, month, day, null);

        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        weight_edit_text.setText(preferences.getString(WEIGHT, ""));
        target_weight_edit_text.setText(preferences.getString(TARGET_WEIGHT, ""));
        target_date_edit_text.setText(preferences.getString(TARGET_DATE, ""));
        name_edit_text.setText(preferences.getString(NAME, ""));
        if (preferences.getBoolean(GENDER, true)) {
            male_option.setChecked(true);
            female_option.setChecked(false);
        } else {
            female_option.setChecked(true);
            male_option.setChecked(false);
        }
        if (preferences.getBoolean(UNITS, true)) {
            kgs_option.setChecked(true);
            lbs_option.setChecked(false);
        } else {
            lbs_option.setChecked(true);
            kgs_option.setChecked(false);
        }
    }

}
