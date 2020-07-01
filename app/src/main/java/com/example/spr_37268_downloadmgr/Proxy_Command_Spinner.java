package com.example.spr_37268_downloadmgr;

import android.view.View;
import android.widget.AdapterView;

public class Proxy_Command_Spinner implements AdapterView.OnItemSelectedListener
{
    private MainActivity m_form;

    public Proxy_Command_Spinner(MainActivity form)
    {
        m_form = form;
    }

    public void onItemSelected (AdapterView<?> parent, View view, int pos, long id)
    {
        Proxy item = (Proxy)parent.getItemAtPosition(pos);
        m_form.SetSelectedProxyCommand(item);
        if (pos == 1) {
            m_form.findViewById(R.id.checkBox).setEnabled(true);
        }
        else{
            m_form.findViewById(R.id.checkBox).setEnabled(false);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
        // nothing
    }

}
