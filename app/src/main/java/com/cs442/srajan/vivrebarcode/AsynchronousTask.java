package com.cs442.srajan.vivrebarcode;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by project on 12/11/17.
 */

public class AsynchronousTask extends AsyncTask<String, String, String> {

    private Context mContext;
    private ClothDetails clothDetails;

    public AsynchronousTask(Context context, ClothDetails clothDetails) {
        mContext = context;
        this.clothDetails = clothDetails;
    }

    public AsynchronousTask() {
        super();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(mContext, "your data is recorded", Toast.LENGTH_LONG).show();
    }

    @Override
    protected String doInBackground(String... arg0) {
        try{

            URL url = new URL("http://altruist.manky.me:3000/posts");

            JSONObject postDataParams = new JSONObject();
            postDataParams.put("token", clothDetails.token);
            postDataParams.put("image_url", clothDetails.url);
            postDataParams.put("category", clothDetails.category);
            postDataParams.put("size", clothDetails.size);
            postDataParams.put("color", clothDetails.colour);
            postDataParams.put("name", clothDetails.clothname);
            postDataParams.put("desc", clothDetails.desc);
            postDataParams.put("url", clothDetails.url);
            postDataParams.put("status", clothDetails.status);

            Log.e("params",postDataParams.toString());

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();

            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                BufferedReader in=new BufferedReader(
                        new InputStreamReader(
                                conn.getInputStream()));
                StringBuffer sb = new StringBuffer("");
                String line="";

                while((line = in.readLine()) != null) {

                    sb.append(line);
                    break;
                }

                in.close();
                return sb.toString();

            }
            else {
                return new String("false : "+responseCode);
            }
        }
        catch(Exception e){
            return new String("Exception: " + e.getMessage());
        }
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}
