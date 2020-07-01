package com.example.spr_37268_downloadmgr;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SPR_37268";

    private TextView status_textview;
    private Spinner choice_spinner;
    private Button action_button;
    private CheckBox proxyCheck;
    private ImageView jpg_image;
    private ArrayAdapter<Proxy> mSpinnerAdapter;
    private Proxy proxy_cmd = null;
    private ConnectivityManager cm = null;
    public ProxyInfo m_proxy_info = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status_textview = (TextView) findViewById(R.id.textView_status);
        action_button = (Button) findViewById(R.id.button_action);
        choice_spinner = (Spinner) findViewById(R.id.spinner1);
        jpg_image = (ImageView)findViewById(R.id.imageView);
        ((Button)findViewById(R.id.button_action)).setOnClickListener(btnClick);
        proxyCheck = findViewById(R.id.checkBox);

        mSpinnerAdapter = new ArrayAdapter<Proxy>(this, android.R.layout.simple_spinner_item);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        choice_spinner.setOnItemSelectedListener(new Proxy_Command_Spinner(this));
        choice_spinner.setAdapter(mSpinnerAdapter);

        mSpinnerAdapter.add(new Proxy(this, Proxy.COMMAND.DOWNLOAD_MGR));
        mSpinnerAdapter.add(new Proxy(this, Proxy.COMMAND.PROXY_DIRECT));
        choice_spinner.setSelection(0);
        proxyCheck.setEnabled(false);

        int pos = choice_spinner.getSelectedItemPosition();
        if (pos >= 0)
        {
            proxy_cmd = (Proxy)mSpinnerAdapter.getItem(choice_spinner.getSelectedItemPosition());
        }
        else
        {
            MY_LOG("onCreate: NO INDEX YET");
        }
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null){
            m_proxy_info = cm.getDefaultProxy();
            if (m_proxy_info != null)
            {
                MY_LOG("onCreate[getDefaultProxy][+]: " + m_proxy_info.toString());
            }
            else
            {
                MY_LOG("onCreate[FAIL]: getDefaultProxy");
            }
        }
/*
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null)
        {*/
/*
            Network[] networks = cm.getAllNetworks();
            if (networks != null)
            {
                MY_LOG("onCreate[getAllNetworks]: " + Integer.toString(networks.length));
                for (Network net : networks)
                {
                    MY_LOG("onCreate[NET]: " + net.toString());
                }
            }
*/
/*
            m_proxy_info = cm.getDefaultProxy();
            if (m_proxy_info != null)
            {
                MY_LOG("onCreate[getDefaultProxy][+]: " + m_proxy_info.toString());
            }
            else
            {
                MY_LOG("onCreate[FAIL]: getDefaultProxy");
            }

            Network network = cm.getActiveNetwork();
            if (network != null)
            {
                MY_LOG("onCreate[getActiveNetwork]: " + network.toString());
            }
            else
            {
                MY_LOG("onCreate[FAIL]: getActiveNetwork");
            }

            if (!cm.bindProcessToNetwork(network))
            {
                MY_LOG("onCreate[FAIL]: bindProcessToNetwork");
            }
            else
            {
                MY_LOG("onCreate[SUCCESS]: bindProcessToNetwork");
            }

            network = cm.getBoundNetworkForProcess();
            if (network != null)
            {
                MY_LOG("onCreate[getBoundNetworkForProcess]: " + network.toString());
            }
            else
            {
                MY_LOG("onCreate[FAIL]: getBoundNetworkForProcess");
            }

            m_proxy_info = cm.getDefaultProxy();
            if (m_proxy_info != null)
            {
                MY_LOG("onCreate[getDefaultProxy][-]: " + m_proxy_info.toString());
            }
            else
            {
                MY_LOG("onCreate[FAIL]: getDefaultProxy");
            }*/

/*
            if (!cm.bindProcessToNetwork(null))
            {
                MY_LOG("onCreate[FAIL]: bindProcessToNetwork");
            }
            else
            {
                MY_LOG("onCreate[SUCCESS]: bindProcessToNetwork");
            }
*/
        /*}
        else
        {
            MY_LOG("onCreate[FAIL]: NO ConnectivityManager");
        }*/
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        MY_LOG("onPause");

        if (proxy_cmd != null)
        {
            proxy_cmd.onPause();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        MY_LOG("onResume");

        if (proxy_cmd != null)
        {
            proxy_cmd.onResume();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Proxy
    //

    public void SetSelectedProxyCommand(Proxy proxy)
    {
        MY_LOG("SetSelectedProxyCommand: " + proxy.toString());
        proxy_cmd = proxy;

        jpg_image.setImageResource(android.R.color.transparent);
    }

    public void EnableAction(boolean enable)
    {
        action_button.setEnabled(enable);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Log functions
    //

    public void MY_LOG(String msg)
    {
        String display = msg + "\n" + status_textview.getText().toString();
        status_textview.setText(display);
        Log.i(TAG, msg);
    }

    public void MY_LOG_ThreadSafe(String msg)
    {
        final String message = msg;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                MY_LOG(message);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Listeners and Broadcast Receivers
    //

    private View.OnClickListener btnClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int id = v.getId();

            if (id == R.id.button_action)
            {
                if (proxy_cmd != null)
                {
                    proxy_cmd.Execute();
                }
            }
        }
    };
}
