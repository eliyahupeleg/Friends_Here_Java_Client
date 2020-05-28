package peleg.eliyahu.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class RequestPermissions extends AppCompatActivity {

    //will save the requested permissions names.
    static String[] per;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //override the all the functions, and use the exist functions.
        super.onCreate(savedInstanceState);

        //show on screen the file: "request_permissions.xml"
        setContentView(R.layout.request_permissions);

        //not using "per" as a parameter of "getPermissions" because the function onRequestPermissionsResult need to call "getPermissions"
        // so the easiest way to save the "per" is from outside.
        per = new String[]{"contacts", "phone_state", "location"};

        //ask the user to enable all the requesting permission- first "about the permissions" dialog, and then- if the user choosing
        // to accept, the request. else, close the app.
        permission_dialog();

    }

    //pop-up message, to tell the user that we gonna ask him for permission.
    public void permission_dialog() {

        //create new builder of alter dialogs- pop-up message.
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);

        //the "spaces" is to center the button.
        //one is "yes" and the second is "no" (positive and negative).

        //do when positive button pressed.
        builder.setPositiveButton("    Got it     ", new DialogInterface.OnClickListener() {

            //to do when positive button pressed
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //if the user accepted to accept the permissions (clicked "yes"), go and ask them with "get_permissions()".
                get_permissions();

            }

        }).
        //do when positive button pressed.
        setNegativeButton("No, thanks       ", new DialogInterface.OnClickListener() {

            //do when positive button pressed.
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //if the user unaccepted to accept the permissions (clicked "no"), close the app.
                finishAffinity();
            }
        });

        //new dialog by the builder from line 49.
        final AlertDialog dialog = builder.create();


        //connect to layout.
        LayoutInflater inflater = getLayoutInflater();

        //new view to access the picture- easier to edit the request as a picture then edit this as a text.
        @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.contact_access_image, null);

        //define the view (the picture) in the dialog.
        dialog.setView(dialogLayout);

        //can't exit from the dialog by the "back" button or tab the screen- should use positive or negative button.
        dialog.setCancelable(false);

        //show the dialog.
        dialog.show();

        //the size of the dialog: 30% on 76% of the screen.
        dialogLayout.getLayoutParams().height = (getWindowManager().getDefaultDisplay().getHeight()/100)*30;
        dialogLayout.getLayoutParams().width = (getWindowManager().getDefaultDisplay().getWidth()/100)*76;

        //show window with the picture, width: 76% of the screen, height: 35% of the screen.
        Objects.requireNonNull(dialog.getWindow()).setLayout((getWindowManager().getDefaultDisplay().getWidth()/100)*76, (getWindowManager().getDefaultDisplay().getHeight()/100)*38);
    }


    //check all the requested permissions, and ask the user if needed.
    public void get_permissions() {

        //the list will save the full names of the requested permissions that not accepted yet, for one purchase request.
        List<String> full_permissions_names = new ArrayList<>();

        ///true if one of the location permission is unable.
        // using boolean to do:
        // if(in_list && [one of the location false])
        // and not:
        // if([in_list && first_location false] || second_location false)
        boolean permissions_location = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        //if need to request location permissions, and the permissions is not accepted.
        if (Arrays.asList(per).contains("location") && permissions_location) {

            //add the full names of the location permission to the permissions list.
            full_permissions_names.add(Manifest.permission.ACCESS_FINE_LOCATION);
            full_permissions_names.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        //if need to request contacts permissions, and the permissions is not accepted.
        if (Arrays.asList(per).contains("contacts") && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            //add the full name of the contact permission to the permissions list.
            full_permissions_names.add(Manifest.permission.READ_CONTACTS);
        }


        //if need to request phone_state permissions, and the permissions is not accepted.
        if (Arrays.asList(per).contains("phone_state") && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            //add the full name of the phone_state permission to the permissions list.
            full_permissions_names.add(Manifest.permission.READ_PHONE_STATE);
        }

        //ask for all the permissions in the "listPermissionsNeeded" list - the miss and needed permissions.
        ActivityCompat.requestPermissions(this, full_permissions_names.toArray(new String[full_permissions_names.size()]), 1);

        //even that if the "requestPermissions" didn't call the "onRequestPermissionsResult" will not run, and the activity will not
        // be finished, you can be sure that if already got all the permissions, this activity will not start.
    }


    //will run when will got the result from the permission request.
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //the requestCode define when the "request_permissions" sent.our is "1".
        switch (requestCode) {

            //do when the request from "get_permissions()" have a result.
            case 1: {

                //if got all the permissions, go back to the last activity.
                if (if_permissions(per)){

                    finish();
                }
                //if steal didn't got all the permissions that needed..
                else{

                    //request again.
                    get_permissions();
                }

            }
        }
    }

    //returning true if all the names of the requesting permissions in "per" is granted, false if not.
    public boolean if_permissions(String[] per){

        //true if one of the location is unable.
        // using boolean to do :
        // if(in_list && [one of the location false])
        // and not:
        // if([in_list && first_location] || second_location)
        boolean permissions_location = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        //if one of the permissions in the list is unable, return false.
        if (Arrays.asList(per).contains("location") && permissions_location ||
                Arrays.asList(per).contains("contacts") && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) ||
                Arrays.asList(per).contains("phone_state") && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){

            return false;
        }
        //if all the permissions in the list is able, return true.
        return true;
    }

    @Override
    //if "back" button pressed, move the app to the background.
    public void onBackPressed()
    { moveTaskToBack(true);}
}
