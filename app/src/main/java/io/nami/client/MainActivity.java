package io.nami.client;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void enable(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_ENABLE, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void isEnabled(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_IS_ENABLED, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getNetworkId(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_NETWORK_ID, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getBalance(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_BALANCE, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getUtxos(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_UTXOS, Uri.parse("cardano://wallet.nami"));
        Bundle bundle = new Bundle();
        bundle.putString("amount", null);
        bundle.putString("paginate", null);
        intent.putExtra("data", bundle);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getCollateral(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_COLLATERAL, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getUsedAddresses(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_USED_ADDRESSES, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getUnusedAddresses(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_UNUSED_ADDRESSES, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getChangeAddress(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_CHANGE_ADDRESS, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void getRewardAddresses(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_REWARD_ADDRESSES, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void signData(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SIGN_DATA, Uri.parse("cardano://wallet.nami"));
        Bundle bundle = new Bundle();
        bundle.putString("address", "002da89217a55434850fdddd3f92e88d9d508e0bb024df66cdaa6e3346e91e38824022980129201f9999a38a577e83a1ed6e2538a6583acb06");
        bundle.putString("payload", "54657374");
        intent.putExtra("data", bundle);
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void signTx(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SIGN_TX, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void submitTx(View view) {
        Intent intent = new Intent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SUBMIT_TX, Uri.parse("cardano://wallet.nami"));
        startActivityForResult(intent, REQUEST_CODE);
    }

    public void dAppBrowser(View view) {
        Intent intent = new Intent(this, BrowserActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);

        if (result == null) {
            Toast.makeText(this, "Oops, Something went wrong...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    String res = "";
                    if (result.getBundleExtra("data") != null) {
                        Bundle data = result.getBundleExtra("data");
                        Set<String> keys = data.keySet();
                        for (String key : keys) {
                            res += "\n" + key + ": " + data.getString(key);
                        };
                    }
                    else if (result.getStringArrayExtra("data") != null) {
                        String[] data = result.getStringArrayExtra("data");
                        for (int i=0;i<data.length;i++) {
                            res += "\n" + i + ": " + data[i];
                        }
                    }
                    else if (result.getStringExtra("data") != null) {
                        res = result.getStringExtra("data");
                    }
                    else if (result.getIntExtra("data", -1) >= 0) {
                        res = result.getIntExtra("data", -1) + "";
                    }
                    else {
                        res = result.getBooleanExtra("data", false) ? "Success" : "Failure";
                    }
                    Toast.makeText(this, "Data: " + res, Toast.LENGTH_SHORT).show();
                }
                else if (resultCode == RESULT_CANCELED) {
                    String err = result.getStringExtra("info");
                    int code = result.getIntExtra("code", 0);
                    Toast.makeText(this, "Error\nCode: " + code + "\nInfo: " + err, Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
