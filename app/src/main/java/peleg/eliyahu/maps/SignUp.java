package peleg.eliyahu.maps;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class SignUp extends AppCompatActivity {

    //"bt_start"- the first "welcome" button.
    //"bt_phone"- get the phone number.
    //"bt_invitation"- get the invitation number.
    //"bt_get_birth_date"- get the birth day date.
    //"bt_get_name_gender"- get the name and the gender.
    Button bt_start, bt_invitation, bt_phone, bt_get_birth_date, bt_get_name_gender;

    //"et_phone"- input from user- his phone number.
    //"et_invitation"- input from user- the invitation number.
    //et_name"- input from user- the name of the user.
    EditText et_invitation, et_phone, et_name;

    //"user_type"- will save if the user is a "User" or "SuperUser". using "U", and "S".
    String user_type;

    //used to get the user birthday.
    DatePicker dp_birthday;

    //"sp_gender"- used to get the user gender.
    //sp_radius"- used to get the radius.
    Spinner sp_gender, sp_radius;

    //"name"- will save the name from the user.
    //"birthday"- will save the user birthday
    //"phone"- will save the user phone
    //"friends_radius"- will save the radius of friends that around. the default is "1", the "SuperUser" can use 1-15.
    //"invitation_num"- will save the invitation number- the friend that invited this user.
    String name, birthday, phone, friends_radius = "1", invitation_num;

    //will save the user's gender.
    char gender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //show on the screen the file: welcome.xml
        setContentView(R.layout.welcome);

        //set the button "welcome" in "welcome.xml"
        bt_start = findViewById(R.id.welcome_button);

        //when "start" clicked
        //all the program running by "bt_start_pressed" function like a string.
        bt_start.setOnClickListener(bt_start_pressed);

    }

    View.OnClickListener bt_get_birth_date_pressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //turn off the button.
            bt_get_birth_date.setClickable(false);

            //show message to the user- wait, working.
            ProgressDialog.show(SignUp.this, "Finishing","Just a few seconds...", true).setCancelable(false);

            //+1 in the month because the counting start in 0 (jan = "0", dec = "11")
            birthday = Integer.toString(dp_birthday.getDayOfMonth()) + "." + Integer.toString(dp_birthday.getMonth() + 1) + "." + Integer.toString(dp_birthday.getYear());

            //get the radius that the user wanted. the radius going back to "0" if the user type is "User" and not "SuperUser".
            friends_radius = sp_radius.getSelectedItem().toString();

            //update the dialog title to "Updating Contacts".
            ProgressDialog.show(SignUp.this, "Updating Contacts","Just a few seconds...", true).setCancelable(false);

            //using one string- for the socket. the server will cut this to different strings every ",".
            MainActivity.DATA_TO_SERVER = name + "," + gender + "," + phone + "," + birthday + "," + user_type + "," + invitation_num + "," + friends_radius + "," + getContacts();

            //send the data to the server.
            new MainActivity.send().start();

            //save the user type into "userType" of "MainActivity".
            MainActivity.userType = user_type;

            //start the thread that waiting until the data will be sent to the server.
            new sending_contacts().start();

        }
    };

    //do when "dp_birthday" date changing.
    final DatePicker.OnDateChangedListener dp_birthday_changed = new DatePicker.OnDateChangedListener() {

        //if the date changed probably it's the user birthday.
        @Override
        public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

            //can to continue only if got the birthday.
            bt_get_birth_date.setEnabled(true);
        }
    };

    //save the data from the user and send it to the server.
    View.OnClickListener bt_get_name_gender_pressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            name = et_name.getText().toString();
            gender = sp_gender.getSelectedItem().toString().charAt(0);

            //show on the screen "sing_up_personal_data.xml"
            setContentView(R.layout.sign_up_birth_date);

            //connect to the items from the xml by they ids
            dp_birthday = findViewById(R.id.birthday_picker);
            bt_get_birth_date = findViewById(R.id.get_birth_date);

            //cant continue without a date.
            bt_get_birth_date.setEnabled(false);

            //if the date changed, probably the user typed his birthday, it's all or nothing.
            dp_birthday.setOnDateChangedListener(dp_birthday_changed);

            //when get_birth_date pressed...
            bt_get_birth_date.setOnClickListener(bt_get_birth_date_pressed);
        }

    };

    //do when "et_name" text changing.
    final TextWatcher et_name_changed = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        //using only the "onTextChanged"
        // because the button status should change while the user typing
        @Override
        public void afterTextChanged(Editable s) {

            //if there is no full name, change the right boolean and unable the button.
            if (s.toString().length() < 5) {

                //cant continue if there is no name.
                bt_get_name_gender.setEnabled(false);
            }

            //min chars in name is 5. 2 for first name, 1 space and 2 for the last name.
            if (s.toString().length() > 4) {

                //enable the button.
                bt_get_name_gender.setEnabled(true);
            }

        }
    };


    //do when "bt_phone" pressed.
    final View.OnClickListener bt_phone_pressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            phone = et_phone.getText().toString();

            //show on the screen: "sing_up_personal_data.xml"
            setContentView(R.layout.sign_up_name_gender);

            //connect to the items from the xml by they ids.
            bt_get_name_gender = findViewById(R.id.get_name_gender);
            et_name = findViewById(R.id.et_name);
            sp_gender = findViewById(R.id.gender_spinner);

            //cant continue if there are no name and birthday.
            bt_get_name_gender.setEnabled(false);

            //do while "et_name" changing..
            et_name.addTextChangedListener(et_name_changed);

            //do when "bt_get_name_gender" pressed.
            bt_get_name_gender.setOnClickListener(bt_get_name_gender_pressed);

        }
    };



    //do when "et_invitation" in use.
    final TextWatcher et_invitation_changed = new TextWatcher() {

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int aft) {}
        @Override public void afterTextChanged(Editable s) {}

        //using only the "onTextChanged", because the button status should change while the user typing.
        @SuppressLint("SetTextI18n")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            //if not typed yet, the user can skip the invitation step and continue as a "User".
            if (s.toString().isEmpty()) {

                //the button is enable- to skip.
                bt_invitation.setEnabled(true);

                //the button text is "skip" and not "next"- the user need to understand that he can type,
                //but he also can skip.
                bt_invitation.setText("skip");

                //if "skip" pressed, the user is a "User".
                user_type = "U";

                //"User" can't change the radius.
                sp_radius.setEnabled(false);

                //back to "0"- if the invitation number deleted only after the radius changed..
                sp_radius.setSelection(0);
            }

            //if the user writing, he can't stop in the middle.
            //to skip again the user need to delete everything from the EditText.
            else if (s.toString().length() < 15) {

                //turn off the button- the user cant continue or skip without full phone number.
                bt_invitation.setEnabled(false);

                //the user can't skip anymore, so he can see "next", but not enable yet.
                bt_invitation.setText("next");

                //the spinner can't changed until the invitation number will be finished.
                sp_radius.setEnabled(false);
                sp_radius.setSelection(0);
            }

            //if the user wrote full invitation number, he can continue as a "SuperUser".
            else if (s.toString().length() == 15) {


                //enable the button, to continue as a "SuperUser".
                bt_invitation.setEnabled(true);

                //changing the text again- sometimes the user will paste full invitation number,
                //so the "setText" before will not run and the button text will stay "skip".
                //the "enable" is ok, but not the text.
                bt_invitation.setText("next");

                //the user type is "S", "SuperUser", he have an invitation number.
                user_type = "S";

                //the radius is on- "SuperUser" can change the radius.
                sp_radius.setEnabled(true);
            }
        }
    };

    //do when "et_phone" text changing.
    final TextWatcher et_phone_changed = new TextWatcher() {

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int aft) {}
        @Override public void afterTextChanged(Editable s) {}

        //using only the "onTextChanged"
        // because the button status should change while the user typing.

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            //the user can't cpntinue without typing phone number.
            if (s.toString().length() < 10) {

                //turn off the button- the user cant continue without full phone number.
                bt_phone.setEnabled(false);
            }

            //if the user wrote full phone number, he can continue.
            else if (s.toString().length() == 10) {

                //enable the button, and now the user can continue.
                bt_phone.setEnabled(true);
            }
        }
    };

    //when "bt_start" pressed..
    View.OnClickListener bt_start_pressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //show on screen the file: sign_up_phone.xml
            setContentView(R.layout.sign_up_invetion);

            //should'nd def "phone" and "phone_button" before because the file "welcome.xml" is on the screen.
            //waiting until "sign_up_phone.xml" will be the content.

            //def the correct EditText to get invitation number from user.
            et_invitation = findViewById(R.id.invitation_num);

            //def the radius on the xml file. used to get the radius from the server- only for "SuperUser".
            sp_radius = findViewById(R.id.radius_spinner);

            //the radius is unable until the user will type his invitation number, and will be a "SuperUser".
            sp_radius.setEnabled(false);

            //def the correct Button to send the data to server.
            bt_invitation = findViewById(R.id.send_invitation);

            //do when "et_invitation" changed (used to enable and unable the radius spinner).
            et_invitation.addTextChangedListener(et_invitation_changed);

            //do when "skip" or "next" choosing..
            bt_invitation.setOnClickListener(bt_invitation_pressed);
        }
    };

    //do when "bt_invitation" pressed.
    View.OnClickListener bt_invitation_pressed= new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //show on screen the sign_up_phone.xml
            setContentView(R.layout.sign_up_phone);

            //save the invitation number into "invitation_num".
            invitation_num = et_invitation.getText().toString();

            //def the correct EditText to get phone from user.
            et_phone = findViewById(R.id.phone_num);

            //def the correct Buttons to send the data to server.
            bt_phone = findViewById(R.id.send_phone);
            bt_phone.setEnabled(false);

            //do when "et_phone" changed.
            et_phone.addTextChangedListener(et_phone_changed);

            //when "next" choosing..
            bt_phone.setOnClickListener(bt_phone_pressed);
        }
    };

    //get all contacts with the template: "name"."number";
    public String getContacts() {

        //will save all the contacts with the template: "name"."number";
        String names_nums = "";

        //new content to get a cursor to get the contacts.
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        //if steel have contacts to read
        if (Objects.requireNonNull(cursor).getCount() > 0) {

            //go to the next contact
            while (cursor.moveToNext()) {

                //get the contact id. used to get the phone and the name.
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                //if the contact have a phone number..
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {

                    //new cursor to get the user with the id "id" data.
                    Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                    //getting all the user "id" names and phones.
                    while (Objects.requireNonNull(cursorInfo).moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        name = name.replace(";", "").replace(".", "");
                        String mobileNumber = cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (!names_nums.contains(name) && !names_nums.contains(mobileNumber)) {
                            names_nums += name + "." + mobileNumber + "." + id + ";";
                        }

                        //Toast.makeText(this, names_nums.length(), Toast.LENGTH_LONG).show();

                    }

                    cursorInfo.close();
                }
            }
            cursor.close();
        }
        return names_nums;
    }

    //running until the server got all the data (unknowing size- the contact can be 9 or 9,000)
    class sending_contacts extends Thread{

        @Override
        //run with the command "sending_contacts.start()".
        public void run() {

            //get the data from the server. the "receive" will wait until the data will come from the server.
            new MainActivity.receive().start();

            //while the data from the server is not "1" yet, the server steal getting the contacts.
            while (!MainActivity.DATA_FROM_SERVER.equals("1")) {}

            //after the server got all the contacts, go to the next activity.
            finish();
        }
    }
    @Override
    //if "back" button pressed, move the app to the background.
    public void onBackPressed()
    { moveTaskToBack(true);}
}