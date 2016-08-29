package com.snaptechnology.bgonzalez.bookmyroomandroid.activity;

/**
 * Created by bgonzalez on 29/08/2016.
 */

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.snaptechnology.bgonzalez.bookmyroomandroid.R;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


public class SocketTestFragment extends Fragment {

    private Handler mHandler;
    private TextView text;
    private int i;



    private ServerSocket serverSocket;

    Handler updateConversationHandler;

    Thread serverThread = null;


    public static final int SERVERPORT = 6000;

    public SocketTestFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_socket_test, container, false);
        int delay = 5000; // delay for 5 sec.
        int period = 1000; // repeat every sec.

        text = (TextView) rootView.findViewById(R.id.socket);

        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());

        this.serverThread.start();

        //mHandler = new Handler();
        //mHandler.post(mUpdate);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private Runnable mUpdate = new Runnable() {
        public void run() {
            Socket mSocket = null;
            String output = "null";
            try
            {
                //tablet
                Socket deviceSocket;
                InetAddress addr = InetAddress.getByName("10.110.10.125");
                ServerSocket serverSocket = new ServerSocket(6500,0,addr);
                while(true) {
                    deviceSocket = serverSocket.accept();
                    InputStream is = deviceSocket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    output = br.readLine();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }


            text.setText("My data: " + output);
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    class ServerThread implements Runnable {

        public void run() {
            System.out.println("Run server thread");
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName("10.110.10.149");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            Socket socket;
            try {
                serverSocket = new ServerSocket(6000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();

                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            text.setText( msg + "\n");
        }
    }

}