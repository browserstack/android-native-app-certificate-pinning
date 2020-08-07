package com.example.androidhttpsconncertpinning;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    final String host = "10.0.2.2"; // secure HTTPs hostname
    final String port = "4443";  // secure HTTPs host port
    final String localhostEndpoint = "https://" + host + ":"  + port; // endpoint accessed by the app
    String uri = "/"; // your secure server endpoint
    final List<String> validHosts = new ArrayList<String>(Arrays.asList("localhost", "10.0.2.2", "bs-local.com")); // adding trusted hosts for local emulator and BrowserStack execution

    Certificate bundledSelfSignedCert = null;
    SSLContext sslContext = null;
    TextView editText = null;
    TextView textView = null;
    Spinner dropdown = null;
    Button buttonGo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        initUI();

        buttonGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText("");
                processButtonGoClick();
            }
        });
    }

    private void processButtonGoClick() {

        try {
            addSelfSignedCertInTrustStore();
            connectWithHTTPSBackend();
        }
        catch (Exception ex) {
            handleAndPrintException(ex);
        }
    }


    public void addSelfSignedCertInTrustStore() throws Exception
    {
        int cert_id = 0;
        if(dropdown.getSelectedItem().toString().equals("valid_cert")) { // sample dummy valid certificate of the secure server
            cert_id = R.raw.valid_cert;
        }
        else if(dropdown.getSelectedItem().toString().equals("invalid_cert")) { // sample dummy invalid certificate
            cert_id = R.raw.invalid_cert;
        }

        bundledSelfSignedCert = CertificateFactory.getInstance("X.509").generateCertificate(getResources().openRawResource(cert_id));

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("Self signed certificate", bundledSelfSignedCert);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    }

    public void connectWithHTTPSBackend() throws Exception {

        final HttpsURLConnection connection = (HttpsURLConnection) new URL(editText.getText().toString()).openConnection();
        final String hostName = host;
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String sslHost, SSLSession sslSession) {

                try {
                    if(validHosts.contains(sslHost)) {
                        return true;
                    }
                }
                catch (Exception ex) {
                    handleAndPrintException(ex);
                }
                return false;
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    StringBuilder builder = new StringBuilder();
                    builder.append(connection.getResponseCode())
                            .append(" ")
                            .append(connection.getResponseMessage())
                            .append("\n");
                    Certificate[] certs = connection.getServerCertificates();
                    for (Certificate cert : certs) {
                        if(bundledSelfSignedCert.toString().equals(certs[0].toString())) {

                            final String server_response = print_content(connection);
                            MainActivity.super.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView.setText(server_response);
                                }
                            });
                        }
                        else {
                            throw new Exception("Connection is not honoured as the bundled certificate does not match the certificate data in the incoming request.");
                        }
                    }
                }
                catch (Exception ex) {
                    handleAndPrintException(ex);
                }
            }
        });

        thread.start();
    }

    private void handleAndPrintException(Exception ex) {

        ex.printStackTrace();
        final List<StackTraceElement> exStackList = Arrays.asList(ex.getStackTrace());
        final String exMessage = ex.getMessage();
        MainActivity.super.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder entireTrace = new StringBuilder();
                entireTrace.append("Message: " +  exMessage +"\n" + "Stacktrace:\n");
                for(StackTraceElement element: exStackList)
                {
                    entireTrace.append(element + "\n");
                }
                textView.setText(entireTrace);

            }
        });
    }

    private String print_content(HttpURLConnection con){
        StringBuilder buffer = new StringBuilder();
        if(con!=null){

            try {

                BufferedReader br =
                        new BufferedReader(
                                new InputStreamReader(con.getInputStream()));

                String input;
                while ((input = br.readLine()) != null){
                    buffer.append(input);
                    System.out.println(input);
                }
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return buffer.toString();

    }

    private void initUI() {

        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText2);
        editText.setText(localhostEndpoint + uri);
        dropdown = findViewById(R.id.spinner1);
        buttonGo = findViewById(R.id.button);
        String[] items = new String[]{"invalid_cert", "valid_cert"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);

    }
}
