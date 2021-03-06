package in.projectmanas.manasliaison.tasks;

import android.app.Activity;
import android.app.Dialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import in.projectmanas.manasliaison.MyCredential;
import in.projectmanas.manasliaison.activities.LoginActivity;
import in.projectmanas.manasliaison.backendless_classes.Sheet;
import in.projectmanas.manasliaison.listeners.SheetDataFetchedListener;

import static in.projectmanas.manasliaison.activities.LoginActivity.REQUEST_GOOGLE_PLAY_SERVICES;

/**
 * Created by knnat on 9/14/2017.
 */

public class ReadSpreadSheet extends AsyncTask<String, Void, ArrayList<ArrayList<ArrayList<String>>>> {
    public SheetDataFetchedListener delegate = null;
    private com.google.api.services.sheets.v4.Sheets mService = null;
    private Exception mLastError = null;
    private List<String> ranges;
    private Activity context;

    public ReadSpreadSheet(GoogleAccountCredential credential, Activity context) {
        this.context = context;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, new MyCredential())
                .setApplicationName("Manas-Liaison")
                .build();
    }


    @Override
    protected ArrayList<ArrayList<ArrayList<String>>> doInBackground(String[] params) {
        try {
            ranges = Arrays.asList(params);
            return getDataFromApi();
        } catch (Exception e) {
            Log.e("Error", e.toString());
            mLastError = e;
            cancel(true);
            return null;
        }
    }


    private ArrayList<ArrayList<ArrayList<String>>> getDataFromApi() throws IOException {
        String spreadsheetId = Sheet.findFirst().getSpreadsheetId();
        //Log.d("Id sheet:", spreadsheetId);
        BatchGetValuesResponse response = this.mService.spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(ranges)
                .execute();

        ArrayList<List<List<Object>>> values = new ArrayList<>();
        for (int i = 0; i < response.getValueRanges().size(); i++) {
            values.add(response.getValueRanges().get(i).getValues());
        }
        ArrayList<ArrayList<ArrayList<String>>> valueStrings = new ArrayList<>();
        if (values.size() > 0) {
            //Log.d("size", values.size() + " ");
            for (int i = 0; i < values.size(); i++) {
                ArrayList<ArrayList<String>> currentTable = new ArrayList<>();
                if (values.get(i) == null)
                    valueStrings.add(currentTable);
                else {
                    for (List row : values.get(i)) {
                        //Log.d("adasd", row.size()+ "");
                        ArrayList<String> currentRow = new ArrayList<>();
                        for (Object ob : row) {
                            //Log.d("Output here", ob.toString());
                            currentRow.add(ob.toString());
                        }
                        currentTable.add(currentRow);
                    }
                    valueStrings.add(currentTable);
                }
            }
        }
        //Log.d("Output recieved of size", valueStrings.size() + "");
        return valueStrings;
    }


    @Override
    protected void onPostExecute(ArrayList<ArrayList<ArrayList<String>>> outputList) {
        //Log.d("Output recieved of size", output.size() + "");
        delegate.onProcessFinish(outputList);
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                context,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onCancelled() {

        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                context.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        LoginActivity.REQUEST_AUTHORIZATION);
            } else {
                Toast.makeText(context, "Please connect to internet", Toast.LENGTH_LONG).show();
                //context.getPreferences(Context.MODE_PRIVATE).edit().clear().apply();
                //mCredential.setSelectedAccount(null);
                //context.startActivity(new Intent(context, LoginActivity.class));
                Log.e("Error", "The following error occurred:\n"
                        + mLastError.getMessage());
            }
        } else {
            Log.e("Error", "Request cancelled.");
        }
    }

}
