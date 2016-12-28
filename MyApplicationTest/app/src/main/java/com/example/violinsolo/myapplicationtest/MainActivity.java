package com.example.violinsolo.myapplicationtest;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.DuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessageSender;
import eneter.messaging.endpoints.typedmessages.IDuplexTypedMessagesFactory;
import eneter.messaging.endpoints.typedmessages.TypedResponseReceivedEventArgs;
import eneter.messaging.messagingsystems.messagingsystembase.IDuplexOutputChannel;
import eneter.messaging.messagingsystems.messagingsystembase.IMessagingSystemFactory;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.messaging.messagingsystems.websocketmessagingsystem.WebSocketMessagingSystemFactory;
import eneter.net.system.EventHandler;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Request message type
    // The message must have the same name as declared in the service.
    // Also, if the message is the inner class, then it must be static.
    public static class MyRequest
    {
        public String Text;
    }

    // Response message type
    // The message must have the same name as declared in the service.
    // Also, if the message is the inner class, then it must be static.
    public static class MyResponse
    {
        public int Length;
    }

    // UI controls
    private Handler myRefresh = new Handler();
    private EditText myMessageTextEditText;
    private EditText myResponseEditText;
    private Button mySendRequestBtn;


    // Sender sending MyRequest and as a response receiving MyResponse.
    private IDuplexTypedMessageSender<MyResponse, MyRequest> mySender;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get UI widgets.
        myMessageTextEditText = (EditText) findViewById(R.id.messageTextEditText);
        myResponseEditText = (EditText) findViewById(R.id.messageLengthEditText);
        mySendRequestBtn = (Button) findViewById(R.id.sendRequestBtn);

        // Subscribe to handle the button click.
        mySendRequestBtn.setOnClickListener(myOnSendRequestClickHandler);

        // Open the connection in another thread.
        // Note: From Android 3.1 (Honeycomb) or higher
        //       it is not possible to open TCP connection
        //       from the main thread.
        Thread anOpenConnectionThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    openConnection();
                }
                catch (Exception err)
                {
                    EneterTrace.error("Open connection failed.", err);
                }
            }
        });
        anOpenConnectionThread.start();
    }

    @Override
    public void onDestroy()
    {
        // Stop listening to response messages.
        mySender.detachDuplexOutputChannel();

        super.onDestroy();
    }

    private void openConnection() throws Exception
    {
        // Create sender sending MyRequest and as a response receiving MyResponse
        IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
        mySender = aSenderFactory.createDuplexTypedMessageSender(MyResponse.class, MyRequest.class);

        // Subscribe to receive response messages.
        mySender.responseReceived().subscribe(myOnResponseHandler);

        // Create TCP messaging for the communication.
        // Note: 10.0.2.2 is a special alias to the loopback (127.0.0.1)
        //       on the development machine.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexOutputChannel anOutputChannel
                = aMessaging.createDuplexOutputChannel("tcp://192.168.1.105:8067/");
        //= aMessaging.createDuplexOutputChannel("tcp://192.168.1.102:8060/");

        // Attach the output channel to the sender and be able to send
        // messages and receive responses.
        mySender.attachDuplexOutputChannel(anOutputChannel);
        Log.i(TAG, "Channel Attached! ----------- ");
    }

    private void onSendRequest(View v)
    {
        // Create the request message.
        final MyRequest aRequestMsg = new MyRequest();
        aRequestMsg.Text = myMessageTextEditText.getText().toString();

        // Send the request message.
        try
        {
            mySender.sendRequestMessage(aRequestMsg);
            Log.i(TAG, "Send Success! - "+aRequestMsg.Text);
        }
        catch (Exception err)
        {
            EneterTrace.error("Sending the message failed.", err);
        }

    }

    private void onResponseReceived(Object sender,
                                    final TypedResponseReceivedEventArgs<MyResponse> e)
    {
        // Display the result - returned number of characters.
        // Note: Marshal displaying to the correct UI thread.
        myRefresh.post(new Runnable()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "Received~ ");
                myResponseEditText.setText(Integer.toString(e.getResponseMessage().Length));
            }
        });
    }

    private EventHandler<TypedResponseReceivedEventArgs<MyResponse>> myOnResponseHandler
            = new EventHandler<TypedResponseReceivedEventArgs<MyResponse>>()
    {
        @Override
        public void onEvent(Object sender,
                            TypedResponseReceivedEventArgs<MyResponse> e)
        {
            onResponseReceived(sender, e);
        }
    };

    private View.OnClickListener myOnSendRequestClickHandler = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            //onSendRequest(v);
            Intent intent = new Intent(MainActivity.this, WebSocketActivity.class);
            startActivity(intent);

        }
    };
}
