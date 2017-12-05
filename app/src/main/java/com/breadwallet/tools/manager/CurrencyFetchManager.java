package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static com.breadwallet.presenter.fragments.FragmentSend.isEconomyFee;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class CurrencyFetchManager {
    private static final String TAG = CurrencyFetchManager.class.getName();

    private static CurrencyFetchManager instance;
    private Timer timer;

    private TimerTask timerTask;
   static String idk = "";

    private Handler handler;

    private CurrencyFetchManager() {
        handler = new Handler();
    }

    public static CurrencyFetchManager getInstance() {

        if (instance == null) {
            instance = new CurrencyFetchManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = getJSonArray(context);
            updateFeePerKb(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();

                    try {




                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");


                        tmp.rate = (float) tmpObj.getDouble("rate");
                        float newRatio = tmp.rate * getBTCRatio(context);

                        BigDecimal bd = new BigDecimal(newRatio);
                        BigDecimal res = bd.setScale(2, RoundingMode.HALF_UP);
                        newRatio = res.floatValue();
                        Log.e("mush", Float.toString(newRatio));

                        tmp.rate = newRatio;
                        tmpObj.put("rate", tmp.rate);


                        String selectedISO = BRSharedPrefs.getIso(context);
                        String tst = Float.toString(tmp.rate);
						Log.e("idc", tst);
//                        Log.e(TAG,"selectedISO: " + selectedISO);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
//                            Log.e(TAG, "theIso : " + theIso);
//                                Log.e(TAG, "Putting the shit in the shared preffs");
                            BRSharedPrefs.putIso(context, tmp.code);
                            BRSharedPrefs.putCurrencyListPosition(context, i - 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }

    public class GetCurrenciesTask extends AsyncTask {
        Set<CurrencyEntity> tmp;
        private Context context;

        public GetCurrenciesTask(Context ctx) {
            this.context = ctx;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if (!BreadApp.isAnyActivityOn()) {
                Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                CurrencyFetchManager.getInstance().stopTimerTask();
            }
            tmp = getCurrencies((Activity) context);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (tmp.size() > 0) {
                CurrencyDataSource.getInstance(context).putCurrencies(tmp);
            }
        }
    }

    private void initializeTimerTask(final Context context) {

        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        new GetCurrenciesTask(context).execute();
                    }
                });
            }
        };
    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask(context);

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    public static float getBTCRatio(Activity activity) {
        String testString = callURL(activity, String.format("https://api.coinmarketcap.com/v1/ticker/bitcoin-cash/"));
        String asdf="";
	   JSONArray jsonArray = null;
        try {





            JSONArray numTick = new JSONArray(testString);
            JSONObject numTickr = null;
            numTickr = numTick.getJSONObject(0);
            asdf = numTickr.getString("price_btc");

        }catch (JSONException ignored) {
        }
        return Float.parseFloat(asdf);
    }

	
	
	
	
	
    public static JSONArray getJSonArray(Activity activity) {
        String jsonString = callURL(activity, String.format("https://%s/rates", BreadApp.HOST));
		String testString = callURL(activity, String.format("https://api.coinmarketcap.com/v1/ticker/bitcoin-cash/"));
;

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONArray numTick = new JSONArray(testString);
            JSONObject numTickr = null;
            numTickr = numTick.getJSONObject(0);
            String asdf = numTickr.getString("price_btc");


            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray("body");


        } catch (JSONException ignored) {
        }
        return jsonArray == null ? getBackUpJSonArray(activity) : jsonArray;
    }

    public static JSONArray getBackUpJSonArray(Activity activity) {
        String jsonString = callURL(activity, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static String convertAddress(Activity activity, String address) {
        String concated = "http://api.unitwallet.co/translateAddy/?addy=" + address;
            String jsonString = callURL(activity, concated);

        String resultAddy = "";
            if (jsonString == null) return null;
            try {
                JSONObject obj = new JSONObject(jsonString);
                resultAddy = obj.getString("resultAddress");
                Log.e("hiMom", resultAddy);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return resultAddy;
        }





        /*
    public static void updateFeePerKb(Activity activity) {
    //No APIs for BCH atm, hard coding until an API is built
        long fee;
        long economyFee;

            fee = 10000;
            economyFee = 7000;
            if (fee != 0 && fee < BRConstants.MAX_FEE_PER_KB) {
                BRSharedPrefs.putFeePerKb(activity, fee);
                BRWalletManager.getInstance().setFeePerKb(fee, isEconomyFee);
            }
            if (economyFee != 0 && economyFee < BRConstants.MAX_FEE_PER_KB) {
                BRSharedPrefs.putEconomyFeePerKb(activity, economyFee);
            }

    }*/

    public static void updateFeePerKb(Activity activity) {
        String jsonString = callURL(activity, "http://api.unitwallet.co/getFees");
        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(TAG, "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }
        long fee;
        long economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = obj.getLong("fee_per_kb");
            economyFee = obj.getLong("fee_per_kb_economy");
            if (fee != 0 && fee < BRConstants.MAX_FEE_PER_KB) {
                BRSharedPrefs.putFeePerKb(activity, fee);
                BRWalletManager.getInstance().setFeePerKb(fee, isEconomyFee);
            }
            if (economyFee != 0 && economyFee < BRConstants.MAX_FEE_PER_KB) {
                BRSharedPrefs.putEconomyFeePerKb(activity, economyFee);
            }
            Log.e("asdfdsa", Float.toString(fee));
            Log.e("asdfdsa", Float.toString(economyFee));

        } catch (JSONException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
    }



    public static String getAddressTranslatedOther(String myAddress) {

        String response;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "http://api.unitwallet.co/translateAddy/?addy=" + myAddress,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String jsonString = response;
                        Log.e ("reply", response);

                        if (jsonString == null) return;
                        try {
                            JSONObject obj = new JSONObject(jsonString);
                            idk = obj.getString("resultAddress");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e ("reply", "error" + error);

                    }
                });
        RequestQueue requestQueue = Volley.newRequestQueue(BreadApp.getBreadContext());

        requestQueue.add(stringRequest);

        response = idk;
        return response;
    }

        private static String callURL(Context app, String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        Log.e("requesteDUrl", myURL);
        StringBuilder sb = new StringBuilder();
        HttpURLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = (HttpURLConnection) url.openConnection();

//            Log.e(TAG, "user agent: " + Utils.getAgentString(app, "android/HttpURLConnection"));
          //  urlConn.setRequestProperty("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"));
            Log.e("btcUr", "Hi");

            urlConn.setReadTimeout(60 * 1000);

            String strDate = urlConn.getHeaderField("date");
         //   if (strDate == null || app == null) {

            if (strDate == null ) {
                Log.e(TAG, "callURL: strDate == null!!!");
            } else {
                @SuppressWarnings("deprecation") long date = Date.parse(strDate) / 1000;
              //  BRSharedPrefs.putSecureTime(app, date);
                Assert.assertTrue(date != 0);
            }

            if (urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);

                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
            assert in != null;
            in.close();
        } catch (Exception e) {
            return null;
        }

        return sb.toString();
    }

}
