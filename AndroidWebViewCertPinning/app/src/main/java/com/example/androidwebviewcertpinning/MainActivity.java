package com.example.androidwebviewcertpinning;

import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    final String host = "10.0.2.2"; // secure HTTPs hostname
    final String port = "4443";  // secure HTTPs host port
    final String localhostEndpoint = "https://" + host + ":"  + port; // endpoint accessed by the app

    TextView editText = null;
    Spinner dropdown = null;
    Button buttonGo = null;
    WebView myWebView = null;
    SslCertificate bundledSelfSignedCert = null;
    boolean isInitUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            initUI();
            initBundleCert();
        }
        catch (Exception ex) {
            handleAndPrintException(ex);
        }

    }

    private void initBundleCert() throws Exception {

        int cert_id = 0;
        if(dropdown.getSelectedItem().toString().equals("valid_cert")) {
            cert_id = R.raw.valid_cert;
        }
        else if(dropdown.getSelectedItem().toString().equals("invalid_cert")) {
            cert_id = R.raw.invalid_cert;
        }

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        InputStream ins = getResources().openRawResource(cert_id);

        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(ins);
        bundledSelfSignedCert =  new SslCertificate(cert);
    }

    public void validateWithBundledCertificate(WebView view, SslErrorHandler handler, SslError error) {

        try {
            System.out.println("Did receive challenge for " + error.getUrl());


            SslCertificate receivedSslCertificate = error.getCertificate();
            String sslCertificateStr = receivedSslCertificate.toString();
            System.out.println("\nCertificate received: " + sslCertificateStr);

            String bundledSslCertificateStr = bundledSelfSignedCert.toString();
            System.out.println("\nCertificate bundled: " + bundledSslCertificateStr);
            if (sslCertificateStr.equals(bundledSslCertificateStr)) {
                System.out.println("\nSuccessfully validated incoming certificate with bundled certificate! Allowing the connection to proceed ...\n\n");
                handler.proceed();
            }
            else {
                throw new Exception("Connection is not honoured as the bundled certificate does not match the certificate data in the incoming request.");
            }
        }
        catch (Exception ex) {
            handleAndPrintException(ex);
        }
    }

    private void initUI() throws Exception {

        if(true)
        {
            setContentView(R.layout.activity_main);
            editText = (EditText) findViewById(R.id.editText2);
            editText.setText(localhostEndpoint);
            dropdown = findViewById(R.id.spinner1);

            dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    processButtonGoClick();
                    try {
                        initBundleCert();
                    } catch (Exception e) {
                        handleAndPrintException(e);
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            buttonGo = findViewById(R.id.button);

            buttonGo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    myWebView.loadData("", null, "UTF-8");
                    processButtonGoClick();
                    try {
                        initBundleCert();
                    } catch (Exception e) {
                        handleAndPrintException(e);
                    }
                }
            });
            String[] items = new String[]{"invalid_cert", "valid_cert"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
            dropdown.setAdapter(adapter);

            processButtonGoClick();
            initBundleCert();

        }

    }

    private void processButtonGoClick() {

        myWebView = (WebView) findViewById(R.id.webview);

        myWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)  {

                validateWithBundledCertificate(view,handler, error);
            }
        });
        System.out.println(editText.getText().toString());
        myWebView.loadData("<html><p><h2><a href='" + editText.getText().toString() + "' />Go to the HTTPS endpoint</a></h2></p></html>", null, "UTF-8");
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
                myWebView.loadData("<html><p><h4>" + entireTrace +"</h4></p></html>", null, "UTF-8");
            }
        });
    }


}
