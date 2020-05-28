package peleg.eliyahu.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {


    //the server data- port and ip.
    static int SERVER_PORT = 8820;
    static String SERVER_IP = "192.168.43.150";

    //true if got data from the server after the "receive" thread ran.
    public static boolean got_from_server;

    //saving the Exception data, if something happened.
    public static String Exception_data;

    //will save the code of the "startActivity". using for "onActivityResult"
    private int RequestPermissionsRequestCode = 1;
    private int ConnectToServerRequestCode = 2;
    private int SignUpRequestCode = 3;

    //new link to the permissions_request activity
    final Intent RequestPermissions = new Intent("peleg.eliyahu.maps.RequestPermissions");

    //new link to the Maps activity of the user.
    final Intent MapsActivityUser = new Intent("peleg.eliyahu.maps.MapsActivityUser");

    //new link to the sign in-up activity
    final Intent SignUp = new Intent("peleg.eliyahu.maps.SignUp");

    //new link to create-socket activity
    final Intent ConnectToServer = new Intent("peleg.eliyahu.maps.ConnectToServer");

    // will save the "imei"- special id of every android device.
    String deviceIMEI;

    //the data from and to the server will be saved here- the easiest way to share the data between the thread and the main.
    public static String DATA_FROM_SERVER;
    public static String DATA_TO_SERVER;

    //when true the socket will be closed by himself.
    static boolean close_socket;

    //will save the socket state.
    static boolean is_socket_connected;

    //will save the name of the needed permissions.
    String[] per;

    //will save the user type. "U" for guest and "S" for user.
    public static String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if did'nt got the all needed permissions, start the "RequestPermissions" activity, to get them.
        //all the explain about the "per" and the "if_permissions()" in the "RequestPermissions" activity.
        //the function is the same, but here the function return boolean.
        //the function copied to here for safe time- if already got the permission, don't go to the another activity.
        per = new String[]{"contacts", "phone_state", "location"};

        //cannot connect to the server without the permissions, so the permissions is the first thing to do.
        //if didn't got all the permissions, run the "RequestPermissions" to complete the missing.
        //when "RequestPermissions" will done, the "ConnectToServer" will run.
        if (!if_permissions(per)) {
            startActivityForResult(RequestPermissions, RequestPermissionsRequestCode);
        }

        //if already got the permissions, connect to the server.
        else {
            startActivityForResult(ConnectToServer, ConnectToServerRequestCode);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if the finished activity is "RequestPermissions"
        if (requestCode == RequestPermissionsRequestCode) {

            //go to the next activity- after got all the permissions, it's possible to connect the server.
            startActivityForResult(ConnectToServer, ConnectToServerRequestCode);

        }
        //if the finished activity is "ConnectToServer", continue with the program- check if the user is "User" or "SuperUser".
        else if (requestCode == ConnectToServerRequestCode) {
            //show message to the user- working on it.
            ProgressDialog.show(MainActivity.this, "Connecting to server", "Just a moment...", true);

            //check if the user is "User" or "SuperUser".
            new check_user_status().start();


        }
        //if the user finished his registering, start the main UI activity (without the "check user", you already know that).
        else if (requestCode == SignUpRequestCode) {

            startActivity(MapsActivityUser);
        }
    }

    //returning true if all the requesting permissions in "per" is granted, false if not.
    public boolean if_permissions(String[] per) {

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

    //returning the special id of every android phone.
    @SuppressLint("HardwareIds")
    public String getDeviceIMEI(Activity activity) {

        //will save the id.
        String deviceUniqueIdentifier = null;

        //add phone manager to get the phone data.
        TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);

        if (null != tm) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

            //save the device id into "deviceUniqueIdentifier"
            deviceUniqueIdentifier = tm.getDeviceId();
        }

        //if there was some problem with the id, return '0', to keep safe from type errors.
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length())
            deviceUniqueIdentifier = "0";

        //return the id as a String
        return deviceUniqueIdentifier;
    }

    //create the socket. using special Thread to keep safe from crashing by closed socket
    static class CreateSocket extends Thread {

        //will use for connection with server.
        static Socket s;

        //the function will run with the line "CreateSocket().start()".
        public void run(){
            try {

                //create the socket and connect to the server with the port and ip from "SERVER_PORT" and "SERVER_IP".
                s = new Socket(SERVER_IP, SERVER_PORT);

                //the last line will stay stock until the socket will be connected- so when going to here the socket connected.
                is_socket_connected = true;

                //run until "close_socket" will be "true"- somewhere in the program the socket asked to be closed.
                //the easiest way to close the socket by the main code.
                while (true){
                    if(close_socket){

                        //close the socket.
                        s.close();

                        //the socket not connected anymore.
                        is_socket_connected = false;

                        //delete the correct thread.
                        Thread.currentThread().interrupt();
                        break;
                    }else{
                        //check once per second
                        Thread.sleep(1000);
                }}


            } catch (Exception e) {
                Exception_data = e.toString();
            }
        }}


    //checking if the user is a "User" or a "SuperUser".
    class check_user_status extends Thread{

        //the function will run with the line "check_user_status().start()".
        public void run() {

            //for the first while loop.
            DATA_FROM_SERVER = "error";

            //run until the user will be organized as a "User", a "SuperUser" or as a new user.
            while (DATA_FROM_SERVER.equals("error")) {

                //get the device special id into deviceIMEI.
                deviceIMEI = getDeviceIMEI(MainActivity.this);

                //send the id to the server. put the data in "DATA_TO_SERVER", and run the "send" thread.
                DATA_TO_SERVER = deviceIMEI;
                new send().start();

                //flag to figure out if got the data from the server.
                DATA_FROM_SERVER = "wait";

                //get from the server the status of the user (found by the id).
                new receive().start();

                //wait until the data from the server got in "DATA_FROM_SERVER".
                while (DATA_FROM_SERVER.equals("wait")){}

                Log.d("user status", DATA_FROM_SERVER);

                //check the user status that got from the server.
                switch (DATA_FROM_SERVER) {

                    //0- new user.
                    case "0": {
                        //sign up - the user is not exist.
                        startActivityForResult(SignUp, SignUpRequestCode);
                        break;
                    }

                    //if the user already exist..
                    case "U":
                    case "S": {
                        //save the type of the user into userType- using in the next activity (only the SuperUser can define  the friends radius).
                        userType = DATA_FROM_SERVER;

                        //start tha main UI activity.
                        startActivity(MapsActivityUser);
                    }
                }
            }
        }
    }

    //sending string to the server by a opened socket.
    static class send extends Thread {

        //the function will run with the line "send().start()".
        public void run() {

            try {

                //the output stream, used for sending data to the server.
                OutputStream out;

                //def the output stream that will send the data to the server. the stream will be define by the socket that created before in "CreateServer".
                out = CreateSocket.s.getOutputStream();

                //send the data "DATA_TO_SERVER" from "MapsActivity" to the server.
                out.write(DATA_TO_SERVER.getBytes());

                //keeping the stream opened- if it'll be closed the server will stop the connection with the client.

            } catch (IOException e) {
                e.printStackTrace();
            }

            //close this thread. is'nt needed anymore.
            Thread.currentThread().interrupt();
        }
    }

    //receiving data from the server into "DATA_FROM_SERVER".
    static class receive extends Thread {

        //the function will run with the line "receive().start()
        public void run() {

            try {

                //create new data stream to get the data from the server.
                InputStream in = CreateSocket.s.getInputStream();

                //will be true after will get the data from the server.
                got_from_server = false;

                //check if the socket is connected before trying to get data.
                if (CreateSocket.s.isBound()) {
                     //save the data from the server into "DATA_FROM_SERVER" in MapsActivity.
                     byte data[] = new byte[1024];
                     int byteRead = in.read(data);
                     DATA_FROM_SERVER = new String (data, 0, byteRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //close this thread. is'nt needed anymore.
            Thread.currentThread().interrupt();
        }

    }

    @Override
    //if "back" button pressed, move the app to the background.
    public void onBackPressed()
    { moveTaskToBack(true);}

}