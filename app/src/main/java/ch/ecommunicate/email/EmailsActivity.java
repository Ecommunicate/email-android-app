package ch.ecommunicate.email;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by amlevin on 8/25/2017.
 */

public class EmailsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private String mIDToken;

    private FirebaseAuth mAuth;

    private static final String TAG="EmailsActivity";

    ListView email_listview;

    EmailArrayAdapter email_array_adapter;

    public class Email {
        String from;
        String subject;
        String body;
        String date;
        String cc;
        Boolean is_read;
        String id;
    }

    List<Email> email_list = null;

    Context context;

    public EmailsActivity() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("new_email")
        );

        update_emails();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //new Chat.ChatAsyncTask1().execute();
            Log.d(TAG,intent.getExtras().getString("contact"));

            update_emails();



        }
    };


    void update_emails() {

        new EmailsProcessor().execute();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent in = getIntent();
        mIDToken = in.getStringExtra("IDToken");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emails);

        context = this;

        email_array_adapter = new EmailArrayAdapter(this, email_list);

        email_listview = (ListView) findViewById(R.id.emailListView);

        email_listview.setOnItemClickListener(this);

    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        Intent mIntent = new Intent(this,ReadOneActivity.class);
        //TextView contact = (TextView) view.findViewById(R.id.contact);
        mIntent.putExtra("email_id", email_list.get(position).id.toString());

        //mIntent.putExtra("contact_username",email_list.get(position).username);
        //mIntent.putExtra("contact_name",email_list.get(position).name);
        mIntent.putExtra("id_token", mIDToken);
        startActivity(mIntent);

        //email_list.get(position).new_message = false;

    }

    private class EmailsProcessor extends AsyncTask<String, Void, Integer> {

        ProgressDialog progressDialog;

        public EmailsProcessor() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //progressDialog = ProgressDialog.show(context, "","Getting Contacts");
        }

        @Override
        protected Integer doInBackground(String... strings) {

            InputStream inputStream = null;
            HttpsURLConnection urlConnection = null;
            Integer result = 0;

            try {
                URL url = new URL("https://email.android.ecommunicate.ch:443/receivedemails/");
                urlConnection = (HttpsURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Content-Type","application/json");

                urlConnection.setRequestProperty("Accept","application/json");

                urlConnection.setRequestMethod("POST");

                urlConnection.setDoInput(true);

                urlConnection.setDoOutput(true);

                OutputStream os = urlConnection.getOutputStream();

                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

                JSONObject token_json = new JSONObject();

                String device_token = FirebaseInstanceId.getInstance().getToken();

                token_json.put("auth_token",mIDToken);

                writer.write(token_json.toString());

                writer.flush();

                writer.close();

                os.close();

                urlConnection.connect();

                int statusCode = urlConnection.getResponseCode();

                if (statusCode == 200) {
                    inputStream = new BufferedInputStream(urlConnection.getInputStream());

                    String response = convertInputStreamToString(inputStream);

                    GsonBuilder gsonBuilder = new GsonBuilder();

                    Gson gson = gsonBuilder.create();
                    email_list = Arrays.asList(gson.fromJson(response, Email[].class));

                    result = 1;

                }
                else {
                    result = 0;

                }

            } catch (Exception e) {

                if (e.getMessage() != null) {
                    Log.d(TAG, e.getMessage());
                }

                if (e.getLocalizedMessage() != null) {
                    Log.d(TAG, e.getLocalizedMessage());
                }

                if (e.getCause() != null) {
                    Log.d(TAG, e.getCause().toString());
                }

                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            email_array_adapter = new EmailArrayAdapter(context, email_list);

            email_listview.setAdapter((ListAdapter) email_array_adapter);

            //progressDialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            //progressDialog.dismiss();
        }
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null){
            result += line;
        }

        if(null!=inputStream){
            inputStream.close();
        }
        return result;
    }

}