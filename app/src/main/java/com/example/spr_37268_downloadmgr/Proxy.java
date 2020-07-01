package com.example.spr_37268_downloadmgr;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Proxy
{
    private static final String TAG = "MainActivity";
    public enum COMMAND
    {
        DOWNLOAD_MGR,
        PROXY_DIRECT
    }

    private COMMAND m_command;
    private MainActivity m_form;
    java.net.Proxy mProxy;
    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //

    Proxy(MainActivity form, COMMAND command)
    {
        m_form = form;
        m_command = command;

        downloadManager = (DownloadManager) m_form.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public COMMAND Get()
    {
        return m_command;
    }

    public void onResume()
    {
        CheckNetwork();
        if (m_command == COMMAND.DOWNLOAD_MGR)
        {
            OnResume_DownloadManager();
        }
        else if (m_command == COMMAND.PROXY_DIRECT)
        {
            OnResume_ProxyDirect();
        }
    }

    public void onPause()
    {
        if (m_command == COMMAND.DOWNLOAD_MGR)
        {
            OnPause_DownloadManager();
        }
        else if (m_command == COMMAND.PROXY_DIRECT)
        {
            OnPause_ProxyDirect();
        }
    }

    public boolean Execute()
    {
        boolean success = true;
        m_form.MY_LOG("========= New Tx ========");
        m_form.MY_LOG("Execute");
        CheckNetwork();

        if (m_command == COMMAND.DOWNLOAD_MGR)
        {
            success = Execute_DownloadManager();
        }
        else if (m_command == COMMAND.PROXY_DIRECT)
        {
           // success = Execute_ProxyDirect();
            downloadZipFile();
        }

        return success;
    }

    @Override
    public String toString()
    {
        String name = "";

        if (m_command == COMMAND.DOWNLOAD_MGR)
        {
            name = "Use Download Manager";
        }
        else if (m_command == COMMAND.PROXY_DIRECT)
        {
            name = "Use RAW HTTP URL";
        }

        return name;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //

    private void CheckNetwork()
    {
        ConnectivityManager conManager = (ConnectivityManager) m_form.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conManager.getActiveNetworkInfo();
        boolean isConnected = ((activeNetwork != null) && (activeNetwork.isConnectedOrConnecting()));

        if (isConnected)
        {
            boolean isWiFi = activeNetwork.getType() == conManager.TYPE_WIFI;
            boolean isMobile = activeNetwork.getType() == conManager.TYPE_MOBILE;

            if (isWiFi)
            {
                m_form.MY_LOG("CheckNetwork[WIFI]");
            }
            else if (isMobile)
            {
                m_form.MY_LOG("CheckNetwork[Mobile]");
            }
        }
        else
        {
            m_form.MY_LOG("CheckNetwork[NOT_CONNECTED]");
        }

        if (null !=  m_form.m_proxy_info) {
            String proxyHost = m_form.m_proxy_info.getHost();
            int proxyPort = m_form.m_proxy_info.getPort();

            try {
                mProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            } catch (Exception e) {

                mProxy = java.net.Proxy.NO_PROXY;
            }
        }else{
            mProxy = java.net.Proxy.NO_PROXY;
        }

        m_form.MY_LOG("doInBackground[PROXY]: " + mProxy.toString());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Download Manager
    //

    DownloadManager downloadManager;
    String downloadFileUrl = "https://1000logos.net/wp-content/uploads/2016/10/Android-Logo.png";
    //String downloadFileUrl = "https://www.googleapis.com/download/storage/v1/b/emm-staging/o/AirwatchAgent20-04.apk?generation=1589559052815498&alt=media";
    private long myDownloadReference;
    private BroadcastReceiver receiverDownloadComplete;
    private BroadcastReceiver receiverNotificationClicked;

    private void OnResume_DownloadManager()
    {
        m_form.MY_LOG("OnResume[+ DOWNLOAD_MGR +]");

        // filter for notifications - only acts on notification while download busy
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);

        receiverNotificationClicked = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String extraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
                long[] references = intent.getLongArrayExtra(extraId);
                for (long reference : references)
                {
                    if (reference == myDownloadReference)
                    {
                        // STEVECOX: do something with the download file
                        m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_NOTIFY[+]");
                    }
                }
            }
        };

        m_form.registerReceiver(receiverNotificationClicked, filter);

        // filter for download - on completion
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        receiverDownloadComplete = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                long reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (myDownloadReference == reference)
                {
                    m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[+]");

                    // STEVECOX: do something with the download file
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(reference);
                    Cursor cursor = downloadManager.query(query);

                    cursor.moveToFirst();
                    // get the status of the download
                    int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(columnIndex);

// <STEVECOX: OLD WAY>
/*
                    String downloadFilePath = "";
                    try {
                        int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                        downloadFilePath = cursor.getString(fileNameIndex);
                        if (downloadFilePath != null) {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS<FILE>: " + downloadFilePath);
                        } else {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS<FILE>: NULL[2]");
                        }
                    }
                    catch (Exception e)
                    {
                        m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS<EXCEPTION>: " + e.getMessage());
                    }
*/
// </STEVECOX: OLD WAY>

// <STEVECOX: NEW WAY>
                    String downloadFilePath = "";

                    m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[1]");

                    String downloadFileLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (downloadFileLocalUri != null)
                    {
                        File mFile = new File(Uri.parse(downloadFileLocalUri).getPath());
                        downloadFilePath = mFile.getAbsolutePath();

                        if (downloadFilePath != null)
                        {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[FILE]: " + downloadFilePath);
                        }
                        else
                        {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[FILE]: NULL[2]");
                        }
                    }
                    else
                    {
                        m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[FILE]: NULL[1]");
                    }

// </STEVECOX: NEW WAY>

                    m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[2]");

                    String downloadTitle = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                    if (downloadTitle != null)
                    {
                        if (downloadFilePath != null)
                        {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[TITLE]: " + downloadTitle);
                        }
                        else
                        {
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[TITLE]: NULL");
                        }
                    }

                    // get the reason - more detail on the status
                    int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(columnReason);

                    switch (status)
                    {
                        case DownloadManager.STATUS_SUCCESSFUL:

                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[SUCCESS]");

                            Bitmap bitmap = BitmapFactory.decodeFile(downloadFilePath);
                            ImageView imageView = (ImageView)m_form.findViewById(R.id.imageView);
                            imageView.setImageBitmap(bitmap);

                            break;
                        case DownloadManager.STATUS_FAILED:
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[FAIL]: " + Integer.toString(reason));
                            break;
                        case DownloadManager.STATUS_PAUSED:
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[PAUSE]: " + Integer.toString(reason));
                            break;
                        case DownloadManager.STATUS_PENDING:
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[PENDING]");
                            break;
                        case DownloadManager.STATUS_RUNNING:
                            m_form.MY_LOG_ThreadSafe("DOWNLOAD_MGR_STATUS[RUNNING]");
                            break;
                    }
                    cursor.close();
                }
            }
        };
        m_form.registerReceiver(receiverDownloadComplete, intentFilter);
    }

    private void OnPause_DownloadManager()
    {
        m_form.MY_LOG("OnPause[+ DOWNLOAD_MGR +]");
        m_form.unregisterReceiver(receiverDownloadComplete);
        m_form.unregisterReceiver(receiverNotificationClicked);
    }

    private boolean Execute_DownloadManager()
    {
        boolean success = true;

        m_form.MY_LOG("EXECUTE[+ DOWNLOAD_MGR +]");

        Uri uri = Uri.parse(downloadFileUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);

        // set the notification
        request.setDescription("My Download").setTitle("Notification Title");

        // Set the path to where to save the file... save in app package directory
        request.setDestinationInExternalFilesDir(m_form, Environment.DIRECTORY_DOWNLOADS, "logo.jpg");

        request.setVisibleInDownloadsUi(true);

        // select which network, etc
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI
                | DownloadManager.Request.NETWORK_MOBILE);


        // queue the download
        myDownloadReference = downloadManager.enqueue(request);

        m_form.MY_LOG("EXECUTE[- DOWNLOAD_MGR -]");

        return success;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Proxy Direct
    //

    private void enableUpdate(final boolean enable)
    {
        m_form.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                m_form.EnableAction(enable);
            }
        });
    }

    private void updateStatus(String msg)
    {
        m_form.MY_LOG_ThreadSafe(msg);
    }

    private void updateProgress(int current, int total)
    {
        final String progress = "DOWNLOAD[" + Integer.toString(current) + "]: " + Integer.toString(total);
        m_form.MY_LOG_ThreadSafe(progress);
    }

    private void OnResume_ProxyDirect()
    {
        m_form.MY_LOG("OnResume[+ PROXY_DIRECT +]");
    }

    private void OnPause_ProxyDirect()
    {
        m_form.MY_LOG("OnPause[+ PROXY_DIRECT +]");
    }

    /*
    private boolean Execute_ProxyDirect()
    {
        ImageView imageView = (ImageView)m_form.findViewById(R.id.imageView);
        new DownloadImageTask(imageView).execute(downloadFileUrl);
        return true;
    }

    private class DownloadImageTask extends AsyncTask<String, String, Bitmap>
    {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage)
        {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls)
        {
            String urlString = urls[0];
            Bitmap bitmap = null;

            publishProgress("doInBackground[urlString]: " + urlString);

            //String proxyHost = "192.168.0.70";
            //int proxyPort = 8080;
            // Proxy proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            //java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            mProxy = null;
            if (null !=  m_form.m_proxy_info) {
                String proxyHost = m_form.m_proxy_info.getHost();
                int proxyPort = m_form.m_proxy_info.getPort();

                try {
                    mProxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                } catch (Exception e) {
                    publishProgress("doInBackground[EXCEPTION]: " + e.getMessage());
                    mProxy = java.net.Proxy.NO_PROXY;
                }
            }else{
                mProxy = java.net.Proxy.NO_PROXY;
            }

            publishProgress("doInBackground[PROXY]: " + mProxy.toString());

            try
            {
                //Uri uri = Uri.parse(urlString);
                //URL url = new URL(uri.toString());
                URL url = new URL(urlString);
                URLConnection conn = url.openConnection(); // STEVECOX
                //URLConnection conn = url.openConnection(proxy); // STEVECOX
                if (!(conn instanceof HttpsURLConnection))
                {
                    publishProgress("doInBackground[FAIL]: !HttpURLConnection");
                    return null;
                }

                HttpsURLConnection  httpConn = (HttpsURLConnection) conn;
                httpConn.setDoOutput(true);
                httpConn.setAllowUserInteraction(false);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod("GET");
                //httpConn.connect();

                boolean redirect = false;
                int response = httpConn.getResponseCode();
                if (response != HttpURLConnection.HTTP_OK)
                {
                    if (
                            (response == HttpURLConnection.HTTP_MOVED_TEMP) ||
                            (response == HttpURLConnection.HTTP_MOVED_PERM) ||
                            (response == HttpURLConnection.HTTP_SEE_OTHER)
                       )
                    {
                        publishProgress("doInBackground[NEEDS_REDIRECT]: " + Integer.toString(response));
                        redirect = true;
                    }
                    else
                    {
                        publishProgress("doInBackground[FAIL][HTTP]: " + Integer.toString(response));
                        return null;
                    }
                }

                if (redirect)
                {
                    // get redirect url from "location" header field
                    String newUrl = httpConn.getHeaderField("Location");

                    // get the cookie if need, for login
                    String cookies = httpConn.getHeaderField("Set-Cookie");

                    publishProgress("doInBackground[newUrl]: " + newUrl);

                    // open the new connnection again
                    httpConn = (HttpsURLConnection) new URL(newUrl).openConnection(mProxy); // STEVECOX
                    //httpConn = (HttpURLConnection) new URL(newUrl).openConnection(); // STEVECOX
                }

                httpConn.connect();
                InputStream in = httpConn.getInputStream();
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
            }
            catch (MalformedURLException e)
            {
                //e.printStackTrace();
                publishProgress("doInBackground[EXCEPTION]: " + e.getMessage());
            }
            catch (IOException e)
            {
                //e.printStackTrace();
                publishProgress("doInBackground[EXCEPTION]: " + e.getMessage());
            }

            return bitmap;
        }

        protected void onProgressUpdate(String... s)
        {
            m_form.MY_LOG(s[0]);
        };

        protected void onPostExecute(Bitmap result)
        {
            if (result == null)
            {
                m_form.MY_LOG("onPostExecute[FAIL]: bitmap == null");
            }
            else
            {
                m_form.MY_LOG("onPostExecute[+++ SUCCESS +++]");
                bmImage.setImageBitmap(result);
            }
        }
    }
*/
    private void downloadZipFile() {
        //https://1000logos.net/wp-content/uploads/2016/10/Android-Logo.png
        RetrofitInterface downloadService = createService(RetrofitInterface.class, "https://1000logos.net/");
        Call<ResponseBody> call = downloadService.downloadFileByUrl("/wp-content/uploads/2016/10/Android-Logo.png");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Got the body for the file");

                    new AsyncTask<Void, String, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... voids) {
                            publishProgress("RetroFit Success Saving file" );
                            Bitmap bitmap = saveToDisk(response.body(), "Android-Logo.png");
                            return bitmap;
                        }

                        protected void onProgressUpdate(String... s)
                        {
                            m_form.MY_LOG(s[0]);
                        };

                        protected void onPostExecute(Bitmap result)
                        {
                            if (result == null)
                            {
                                m_form.MY_LOG("onPostExecute[FAIL]: bitmap == null");
                            }
                            else
                            {
                                m_form.MY_LOG("onPostExecute[+++ SUCCESS +++]");
                                ImageView imageView = (ImageView)m_form.findViewById(R.id.imageView);
                                imageView.setImageBitmap(result);
                            }
                        }
                    }.execute();

                } else {
                    Log.d(TAG, "Connection failed " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, t.getMessage());
            }
        });

    }

    private Bitmap saveToDisk(ResponseBody body, String filename) {
        File destinationFile;
        try {
            new File("/data/data/" + m_form.getPackageName() + "/games").mkdir();
            //File destinationFile = new File("/data/data/" + getPackageName() + "/games/" + filename);
             destinationFile = new File("/data/tmp/public/" + filename);


            if (destinationFile.exists()){
                destinationFile.delete();
            }

            InputStream is = null;
            OutputStream os = null;

            try {
                long filesize = body.contentLength();
                Log.d(TAG, "File Size=" + filesize);
                is = body.byteStream();
                os = new FileOutputStream(destinationFile);

                byte data[] = new byte[4096];
                int count;
                int progress = 0;
                while ((count = is.read(data)) != -1) {
                    os.write(data, 0, count);
                    progress +=count;
                    Log.d(TAG, "Progress: " + progress + "/" + filesize + " >>>> " + (float) progress/filesize);
                }

                os.flush();

                Log.d(TAG, "File saved successfully!");

                Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(destinationFile));

                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Failed to save the file!");
                return null;
            } finally {
                if (is != null) is.close();
                if (os != null) os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to save the file!");
            return null;
        }
    }



    public <T> T createService(Class<T> serviceClass, String baseUrl) {
        OkHttpClient client;
        final CheckBox checkBox = (CheckBox) m_form.findViewById(R.id.checkBox);
        if (checkBox.isChecked()){
            client = new OkHttpClient.Builder().proxy(mProxy).build();
        }
        else{
            client = new OkHttpClient.Builder().build();
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .build();
        return retrofit.create(serviceClass);
    }

}
