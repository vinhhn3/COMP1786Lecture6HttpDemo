package com.example.comp1786lecture6httpdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    private WebView browser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        browser = findViewById(R.id.webkit);

        try{
            URL pageURL = new URL(getString(R.string.url));
            HttpURLConnection con = (HttpURLConnection) pageURL.openConnection();

            String jsonString = getString(R.string.json);

            JsonThread myTask = new JsonThread(this, con, jsonString);
            Thread t1 = new Thread(myTask, "JSON Thread");
            t1.start();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    class JsonThread implements Runnable{
        private AppCompatActivity activity;
        private HttpURLConnection con;
        private String jsonPayload;

        JsonThread(AppCompatActivity activity, HttpURLConnection con, String jsonPayload) {
            this.activity = activity;
            this.con = con;
            this.jsonPayload = jsonPayload;
        }

        @Override
        public void run() {
            String response = "";
            if (prepareConnection()){
                response = postJson();
            }
            else {
                response = "Error preparing the connection";
            }

            showResult(response);
        }

        private void showResult(String response) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String page = generatePage(response);
                    Log.i("xxxxx", page);
                    ((MainActivity)activity).browser.loadData(
                            page,
                            "text/html",
                            "UTF-8"
                    );
                }
            });
        }

        private String generatePage(String content) {
            return "<html><body><h1>" + content + "</h1></body></html>";
        }

        private String postJson() {
            String response = "";
            try {
                String postParameters = "jsonpayload=" + URLEncoder.encode(jsonPayload, "UTF-8");
                con.setFixedLengthStreamingMode(postParameters.getBytes().length);
                PrintWriter out = new PrintWriter(con.getOutputStream());
                out.print(postParameters);
                out.close();
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response = readStream(con.getInputStream());
                } else {
                    response = "Error contacting server: " + responseCode;
                }
            } catch (Exception e) {
                response = e.toString();//"Error executing code";
            }
            return response;
        }

        private String readStream(InputStream in) {
            StringBuilder sb = new StringBuilder();
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String nextLine = "";
                while((nextLine = reader.readLine()) != null){
                    sb.append(nextLine);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }

            return sb.toString();
        }
        private boolean prepareConnection() {
            try{
                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                return true;
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private static class GetAndDisplayThread implements Runnable{
        private final HttpURLConnection con;
        private final AppCompatActivity activity;

        private GetAndDisplayThread(HttpURLConnection con, AppCompatActivity activity) {
            this.con = con;
            this.activity = activity;
        }

        @Override
        public void run() {
            String response = "";
            try{
                response = readStream(con.getInputStream());
            }
            catch (IOException e){
                e.printStackTrace();
            }

            String requiredData ="";
            try{
                requiredData = extractRequiredData(response);
            }
            catch (Exception e){
                e.printStackTrace();
            }

            showResult(requiredData);
        }

        private void showResult(String response) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String page = generatePage(response);
                    Log.i("xxxxx", page);
                    ((MainActivity)activity).browser.loadData(
                            page,
                            "text/html",
                            "UTF-8"
                    );
                }
            });
        }

        private String generatePage(String content) {
            return "<html><body><h1>" + content + "</h1></body></html>";
        }

        private String extractRequiredData(String responseBody) throws Exception {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document embeddedDoc = builder.parse(new InputSource(new StringReader(responseBody)));

            NodeList titleNodes = embeddedDoc.getElementsByTagName("title");
            if (titleNodes != null){
                Element titleElement = (Element) titleNodes.item(0);
                titleElement.normalize();
                Node titleContent = titleElement.getFirstChild();
                return titleContent.getNodeValue();
            }

            return "";
        }

        private String readStream(InputStream in) {
            StringBuilder sb = new StringBuilder();
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String nextLine = "";
                while((nextLine = reader.readLine()) != null){
                    sb.append(nextLine);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }

            return sb.toString();
        }
    }


}