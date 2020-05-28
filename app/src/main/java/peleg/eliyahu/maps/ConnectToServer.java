package peleg.eliyahu.maps;

import android.app.ProgressDialog;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ConnectToServer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //override the all the functions, and use the exist functions.
        super.onCreate(savedInstanceState);

        //show on screen the file "activity_connect_to_server.xml"
        setContentView(R.layout.activity_connect_to_server);

        //start the connection with the server,
        // this socket will be used for the future connections with the server(send and receive).
        // using the "CreateSocket" in "MainActivity" to access the socket from other activity and other threads.
        new MainActivity.CreateSocket().start();

        //show message to user that saying "waiting for server".
        ProgressDialog.show(this, "Connecting to server","Just a few seconds...", true);

        //the thread will wait until the socket will be connected and then closing the activity.
        new is_socket_connected().start();
    }

    //using Thread for able the "ProgressDialog" will run while waiting for server.
    class is_socket_connected extends Thread{

        public void run(){

            //wait until the socket will connect to the server.
            while (!MainActivity.is_socket_connected){}

            //when the socket connected, the activity is use less.
            finish();
        }
    }

    @Override
    //if "back" button pressed, move the app to the background.
    public void onBackPressed()
    { moveTaskToBack(true);}
}
