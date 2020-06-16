package com.softaai.sslhandshakedemoapp;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    public SSLContext context1 = null;
    private Api exampleApi;
    String exampleUrl = "https://www.google.com";
    TextView mainTextView;
    ScrollView mainTextScroller;

    String caCertificateName = "master-cacert.pem";

    String clientCertificateName = "client-cert.p12";

    String clientCertificatePassword = "chariot";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainTextView = findViewById(R.id.mainText);
        mainTextScroller = findViewById(R.id.mainTextScroller);
    }


    @Override
    protected void onResume() {
        super.onResume();

        //doRequest();
        new connection().execute();
    }

    private void updateOutput(String text) {
        mainTextView.setText(mainTextView.getText() + "\n\n" + text);
    }


    private class connection extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            //connect();
            int output = 0;
            try{
                output = check();
            }catch (IOException e){
                e.printStackTrace();
            }
            return output;
        }

        @Override
        protected void onPostExecute(Object result){
            System.out.println(result);
            updateOutput(result.toString());
//            if(result.equals(200)){
//                Intent intent_name = new Intent();
//               //intent_name.setClass(getApplicationContext(), Successful.class);
//                startActivity(intent_name);
//            }
        }
    }

    private int check() throws IOException{
        HttpsURLConnection urlConnection = null;

        try{
            Certificate ca = null;
            URL url = new URL("https://solutions-qa.riverbed.cc"); // https://solutions-qa.riverbed.cc/
            SSLContext sslCtx = SSLContext.getInstance("TLS");
            sslCtx.init(null, new TrustManager[]{new X509TrustManager() {
                private X509Certificate[] accepted;

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                     accepted = chain;
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    //return new X509Certificate[0];
                    return accepted;
                }
            }}, null);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            connection.setSSLSocketFactory(sslCtx.getSocketFactory());

            if(connection.getResponseCode() == 200){
                Certificate[] certificates = connection.getServerCertificates();
                ca = certificates[0];
            }

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);

            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            //Create an SSLContext that uses our TrustManager
            context1 = SSLContext.getInstance("TLS");
            context1.init(null, tmf.getTrustManagers(), null);

            // We have another URL we procced it with basic auth, username and password
            url = new URL("https://solutions-qa.riverbed.cc/api/scm.config/1.0/orgs");

            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            try{
               urlConnection = (HttpsURLConnection) url.openConnection();
               final String basicAuth = "Basic" + Base64.encodeToString("user:pass".getBytes(), Base64.NO_WRAP);
               urlConnection.setRequestProperty("Authorization", basicAuth);
            }catch (IOException e){
                e.printStackTrace();
            }

            urlConnection.setSSLSocketFactory(context1.getSocketFactory());
            try{
             System.out.println(urlConnection.getResponseMessage());
             System.out.println(urlConnection.getResponseCode());
             if(urlConnection.getResponseCode() == 200){
                 InputStream in = urlConnection.getInputStream();
                 System.out.println(in.read());
                 String line;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                 StringBuilder out = new StringBuilder();
                 while((line = reader.readLine()) != null){
                     out.append(line);
                 }
                 System.out.println(out.toString());
             }
            }catch(IOException e){
                e.printStackTrace();
            }

            connection.disconnect();

        }catch(IOException e){
          e.printStackTrace();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }catch(CertificateEncodingException e){
            e.printStackTrace();
        }catch(KeyManagementException e){
            e.printStackTrace();
        }catch(CertificateException e){
            e.printStackTrace();
        }catch(KeyStoreException e){
            e.printStackTrace();
        }

        return urlConnection.getResponseCode();
    }






















    private void doRequest() {

        try {
            AuthenticationParameters authParams = new AuthenticationParameters();
            authParams.setClientCertificate(getClientCertFile());
            authParams.setClientCertificatePassword(clientCertificatePassword);
            authParams.setCaCertificate(readCaCert());

            exampleApi = new Api(authParams);
            updateOutput("Connecting to " + exampleUrl);

            new AsyncTask() {
                @Override
                protected Object doInBackground(Object... objects) {

                    try {
                        String result = exampleApi.doGet(exampleUrl);
                        int responseCode = exampleApi.getLastResponseCode();
                        if (responseCode == 200) {
                            publishProgress(result);
                        } else {
                            publishProgress("HTTP Response Code: " + responseCode);
                        }

                    } catch (Throwable ex) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintWriter writer = new PrintWriter(baos);
                        ex.printStackTrace(writer);
                        writer.flush();
                        writer.close();
                        publishProgress(ex.toString() + " : " + baos.toString());
                    }

                    return null;
                }

                @Override
                protected void onProgressUpdate(final Object... values) {
                    StringBuilder buf = new StringBuilder();
                    for (final Object value : values) {
                        buf.append(value.toString());
                    }
                    updateOutput(buf.toString());
                }

                @Override
                protected void onPostExecute(final Object result) {
                    updateOutput("Done!");
                }
            }.execute();

        } catch (Exception ex) {
            Log.e("main activity", "failed to create timeApi", ex);
            updateOutput(ex.toString());
        }
    }

    private File getClientCertFile() {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        return new File(externalStorageDir, clientCertificateName);
    }

    private String readCaCert() throws Exception {
        AssetManager assetManager = getAssets();
        InputStream inputStream = assetManager.open(caCertificateName);
        return IOUtil.readFully(inputStream);
    }
}
