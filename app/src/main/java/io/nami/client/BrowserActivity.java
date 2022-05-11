package io.nami.client;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class BrowserActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebView.setWebContentsDebuggingEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        String wallet = "nami";
        String walletName = "Nami";
        webView.addJavascriptInterface(new DAppConnectorInterface(wallet, webView), walletName);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String walletApiJs =
                        "  %1$s: {\n" +
                        "    name: '%2$s',\n" +
                        "    enable: async function () {\n" +
                        "      await callApiMethod(function() { return %2$s.enable(); });\n" +
                        "      const api = {\n" +
                        "        getNetworkId: generateApi(function() { return %2$s.getNetworkId() }),\n" +
                        "        getBalance: generateApi(function() { return %2$s.getBalance() }),\n" +
                        "        getUtxos: function (amount = null, paginate) {\n" +
                        "          return callApiMethod(function() { return %2$s.getUtxos(amount, paginate?.page, paginate?.limit); });\n" +
                        "        },\n" +
                        "        getUsedAddresses: generateApi(function() { return %2$s.getUsedAddresses() }),\n" +
                        "        getUnusedAddresses: generateApi(function() { return %2$s.getUnusedAddresses() }),\n" +
                        "        getChangeAddress: generateApi(function() { return %2$s.getChangeAddress() }),\n" +
                        "        getRewardAddresses: generateApi(function() { return %2$s.getRewardAddresses() }),\n" +
                        "        signData: function (address, payload) {\n" +
                        "          return callApiMethod(function() { return %2$s.signData(address, payload); });\n" +
                        "        },\n" +
                        "        signTx: function (tx, partialSign = false) {\n" +
                        "          return callApiMethod(function() { return %2$s.signTx(tx, partialSign); });\n" +
                        "        },\n" +
                        "        submitTx: function (tx) {\n" +
                        "          return callApiMethod(function() { return %2$s.submitTx(tx); });\n" +
                        "        },\n" +
                        "        experimental: {\n" +
                        "          getCollateral: generateApi(function() { return %2$s.getCollateral() }),\n" +
                        "        },\n" +
                        "      };\n" +
                        "      return api;\n" +
                        "    },\n" +
                        "    isEnabled: generateApi(function() { return %2$s.isEnabled() }),\n" +
                        "  },\n";
                String javascript =
                        "callApiMethod = function (apiMethod) {\n" +
                        "  return new Promise(function (resolve, reject) {\n" +
                        "    let json = apiMethod();\n" +
                        "    let response = JSON.parse(json);\n" +
                        "    if (response.error) {\n" +
                        "      let { code, info } = response;\n" +
                        "      reject({ code, info });\n" +
                        "    }\n" +
                        "    else resolve(response.data);\n" +
                        "  });\n" +
                        "};\n" +
                        "generateApi = function (apiMethod) {\n" +
                        "  return function () {\n" +
                        "    return callApiMethod(apiMethod);\n" +
                        "  };\n" +
                        "};\n" +
                        "window.cardano = {\n" +
                        String.format(walletApiJs, wallet, walletName) +
                        "};";
                view.evaluateJavascript(javascript, (res) -> {});
            }
        });
        webView.loadUrl("about:blank");
        webView.loadUrl("caart.store"); // testnet.epoch.art
    }

    public class DAppConnectorInterface {
        private String wallet;
        private WebView webView;
        private ActivityResultLauncher<Intent> activityLauncher;
        private Intent result = null;
        private Integer resultCode = null;

        DAppConnectorInterface(String wallet, WebView webView) {
            this.wallet = wallet;
            this.webView = webView;

            activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                activityResult -> {
                    result = activityResult.getData();
                    resultCode = activityResult.getResultCode();
                }
            );
        }

        private Intent getIntent(String action) {
            return new Intent(action, Uri.parse("cardano://wallet." + wallet));
        }

        private String getOrigin(String url) {
            Uri uri = Uri.parse(url);
            String origin = "";
            if (uri.getPort() == -1){
                origin = uri.getScheme() + "://" + uri.getHost();
            }
            else {
                origin = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
            }
            return origin;
        }

        private void launchActivity(Intent intent) {
            result = null;
            resultCode = null;
            webView.post(() -> {
                String url = webView.getUrl();
                String origin = getOrigin(url);
                intent.putExtra("origin", origin);

                activityLauncher.launch(intent);
            });

            while (resultCode == null) {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public Map<String, Object> convertToJsObject(Bundle bundle) {
            Map<String, Object> map = new HashMap<>();
            for (String key : bundle.keySet()) {
                map.put(key, bundle.get(key));
            }
            return map;
        }

        @JavascriptInterface
        public String enable() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_ENABLE);
            launchActivity(intent);

            Response<Boolean> response;
            if (result == null) {
                response = new Response<>(0, "Please install the App to proceed");
                return response.toJson();
            }

            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getBooleanExtra("data", false));
            return response.toJson();
        }

        @JavascriptInterface
        public String isEnabled() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_IS_ENABLED);
            launchActivity(intent);

            Response<Boolean> response;
            if (result == null) {
                response = new Response<>(0, "Please install the App to proceed");
                return response.toJson();
            }

            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getBooleanExtra("data", false));
            return response.toJson();
        }

        @JavascriptInterface
        public String getNetworkId() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_NETWORK_ID);
            launchActivity(intent);

            Response<Integer> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getIntExtra("data", 0));
            return response.toJson();
        }

        @JavascriptInterface
        public String getBalance() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_BALANCE);
            launchActivity(intent);

            Response<String> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getUtxos(String amount, Integer page, Integer limit) {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_UTXOS);
            Bundle bundle = new Bundle();
            bundle.putString("amount", amount);
            Bundle paginate = null;
            if (page != null) {
                paginate = new Bundle();
                paginate.putInt("page", page);
                paginate.putInt("limit", limit);
            }
            bundle.putBundle("paginate", paginate);
            intent.putExtra("data", bundle);
            launchActivity(intent);

            Response<String[]> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringArrayExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getCollateral() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_COLLATERAL);
            launchActivity(intent);

            Response<String[]> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringArrayExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getUsedAddresses() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_USED_ADDRESSES);
            launchActivity(intent);

            Response<String[]> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringArrayExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getUnusedAddresses() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_UNUSED_ADDRESSES);
            launchActivity(intent);

            Response<String[]> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringArrayExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getChangeAddress() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_CHANGE_ADDRESS);
            launchActivity(intent);

            Response<String> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String getRewardAddresses() {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_GET_REWARD_ADDRESSES);
            launchActivity(intent);

            Response<String[]> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringArrayExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String signData(String address, String payload) {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SIGN_DATA);
            Bundle bundle = new Bundle();
            bundle.putString("address", address);
            bundle.putString("payload", payload);
            intent.putExtra("data", bundle);
            launchActivity(intent);

            Response<Map<String, Object>> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(convertToJsObject(result.getBundleExtra("data")));
            return response.toJson();
        }

        @JavascriptInterface
        public String signTx(String tx, boolean partialSign) {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SIGN_TX);
            Bundle bundle = new Bundle();
            bundle.putString("tx", tx);
            bundle.putBoolean("partialSign", partialSign);
            intent.putExtra("data", bundle);
            launchActivity(intent);

            Response<String> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringExtra("data"));
            return response.toJson();
        }

        @JavascriptInterface
        public String submitTx(String tx) {
            Intent intent = getIntent(BuildConfig.CARDANO_WALLET_INTENT_ACTION_SUBMIT_TX);
            Bundle bundle = new Bundle();
            bundle.putString("tx", tx);
            intent.putExtra("data", bundle);
            launchActivity(intent);

            Response<String> response;
            if (resultCode == RESULT_CANCELED)
                response = new Response<>(result.getIntExtra("code", 0), result.getStringExtra("info"));
            else
                response = new Response<>(result.getStringExtra("data"));
            return response.toJson();
        }
    }
}
