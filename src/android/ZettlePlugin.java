package io.zettle;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ProcessLifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.izettle.android.auth.IZettleAuth;
import com.izettle.payments.android.payment.TransactionReference;
import com.izettle.payments.android.payment.refunds.CardPaymentPayload;
import com.izettle.payments.android.payment.refunds.RefundsManager;
import com.izettle.payments.android.payment.refunds.RetrieveCardPaymentFailureReason;
import com.izettle.payments.android.sdk.IZettleSDK;
import com.izettle.payments.android.sdk.User;
import com.izettle.payments.android.ui.SdkLifecycle;
import com.izettle.payments.android.ui.payment.CardPaymentActivity;
import com.izettle.payments.android.ui.payment.CardPaymentResult;
import com.izettle.payments.android.ui.readers.CardReadersActivity;
import com.izettle.payments.android.ui.refunds.RefundResult;
import com.izettle.payments.android.ui.refunds.RefundsActivity;

import java.util.UUID;

import static com.izettle.android.commons.ext.state.StateExtKt.toLiveData;

public class ZettlePlugin extends CordovaPlugin {
    private static int REQUEST_CODE_PAYMENT = 1001;
    private static int REQUEST_CODE_REFUND = 1002;
    private static int REQUEST_CODE_SETTING = 1003;


    private TextView loginStateText;
    private Button loginButton;
    private Button logoutButton;
    private Button chargeButton;
    private Button refundButton;
    private Button settingsButton;
    private EditText amountEditText;
    private CheckBox tippingCheckBox;
    private CheckBox loginCheckBox;
    private CheckBox installmentsCheckBox;
    private MutableLiveData<String> lastPaymentTraceId;
    private CallbackContext callback = null;
    private double Amount = 0;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Log.wtf("hej","made it to the plugin");
        Log.wtf("hej",action);

        String clientId = "1c702275-51dc-4790-8cdc-f2b0ac35cb3c";
        String redirectUrl = "yourpay://pos";
        IZettleSDK.Instance.init(cordova.getContext(), clientId, redirectUrl);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new SdkLifecycle(IZettleSDK.Instance));

        if(action.equals("login")) {
            onLoginClicked();
        }

        if (action.equals("pay")) {
            Double amountEditText = null;
            try {
                amountEditText = Double.parseDouble(args.get(0).toString());
            } catch (Exception e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Can't parse amount"));
                return false;
            }
            Amount = amountEditText;
            onChargeClicked();
        }

        if (action.equals("payWithToken")) {

        }

        if(action.equals("settings")) {
            onSettingsClicked();
        }

        if(action.equals("logout")) {
            onLogoutClicked();
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PAYMENT && data != null) {
            CardPaymentResult result = data.getParcelableExtra(CardPaymentActivity.RESULT_EXTRA_PAYLOAD);
            if (result instanceof CardPaymentResult.Completed) {
                Toast.makeText(this.cordova.getActivity(), "Payment completed", Toast.LENGTH_SHORT).show();
            } else if (result instanceof CardPaymentResult.Canceled) {
                Toast.makeText(this.cordova.getActivity(), "Payment canceled", Toast.LENGTH_SHORT).show();
            } else if (result instanceof CardPaymentResult.Failed) {
                Toast.makeText(this.cordova.getActivity(), "Payment failed ", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_CODE_REFUND && data != null) {
            RefundResult result = data.getParcelableExtra(RefundsActivity.RESULT_EXTRA_PAYLOAD);
            if (result instanceof RefundResult.Completed) {
                Toast.makeText(this.cordova.getActivity(), "Refund completed", Toast.LENGTH_SHORT).show();
            } else if (result instanceof RefundResult.Canceled) {
                Toast.makeText(this.cordova.getActivity(), "Refund canceled", Toast.LENGTH_SHORT).show();
            } else if (result instanceof RefundResult.Failed) {
                Toast.makeText(this.cordova.getActivity(), "Refund failed ", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private void onUserAuthStateChanged(boolean isLoggedIn) {
        loginStateText.setText("State: " + (isLoggedIn ? "Authenticated" : "Unauthenticated"));
        loginButton.setEnabled(!isLoggedIn);
        logoutButton.setEnabled( isLoggedIn);
    }

    private void onLoginClicked() {
        IZettleSDK.Instance.getUser().login(this.cordova.getActivity());
    }

    private void onLogoutClicked() {
        IZettleSDK.Instance.getUser().logout();
    }

    private void onChargeClicked() {
        long amountEditTextContent = (long) Amount;
        //String amountEditTextContent = Amount.toString();
        Log.wtf("hej", String.valueOf(amountEditTextContent));

        String internalTraceId = UUID.randomUUID().toString();
        long amount = amountEditTextContent;
        //boolean enableTipping = tippingCheckBox.isChecked();
        //boolean enableInstallments = installmentsCheckBox.isChecked();
        //boolean enableLogin = loginCheckBox.isChecked();

        TransactionReference reference = new TransactionReference.Builder(internalTraceId)
                .put("PAYMENT_EXTRA_INFO", "Started from home screen")
                .build();

        Intent intent = new CardPaymentActivity.IntentBuilder(this.cordova.getActivity())
                .amount(amount)
                .reference(reference)
                //.enableTipping(enableTipping) // Only for markets with tipping support
                //.enableInstalments(enableInstallments) // Only for markets with installments support
                .enableLogin(true) // Mandatory to set
                .build();

        cordova.startActivityForResult(this,intent, REQUEST_CODE_PAYMENT);
        lastPaymentTraceId.setValue(internalTraceId);
    }

    private void onSettingsClicked() {
        cordova.startActivityForResult(this,CardReadersActivity.newIntent(cordova.getContext()),REQUEST_CODE_SETTING);
    }

    private void onRefundClicked() {
        String internalTraceId = lastPaymentTraceId.getValue();
        if (internalTraceId == null) {
            return;
        }

        IZettleSDK.Instance.getRefundsManager().retrieveCardPayment(internalTraceId, new RefundCallback());
    }

    private class RefundCallback implements RefundsManager.Callback<CardPaymentPayload, RetrieveCardPaymentFailureReason> {

        @Override
        public void onFailure(RetrieveCardPaymentFailureReason reason) {
            Toast.makeText(cordova.getContext(), "Refund failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSuccess(CardPaymentPayload payload) {
            TransactionReference reference = new TransactionReference.Builder(payload.getReferenceId())
                    .put("REFUND_EXTRA_INFO", "Started from home screen")
                    .build();

            Intent intent = new RefundsActivity.IntentBuilder(cordova.getContext())
                    .cardPayment(payload)
                    .receiptNumber("#123456")
                    .taxAmount(payload.getAmount() / 10)
                    .reference(reference)
                    .build();

            //cordova.startActivityForResult(cordova.,intent, REQUEST_CODE_REFUND);

            JSONObject res = new JSONObject();
            try {
                res.put("code", REQUEST_CODE_REFUND);
                res.put("message", intent);
            } catch (Exception e) {}

            PluginResult result = new PluginResult(PluginResult.Status.OK, res);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);

        }
    }
}
