package com.donotauthenticatemyapp.teamaccountmanager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import mehdi.sakout.fancybuttons.FancyButton;

import static com.donotauthenticatemyapp.teamaccountmanager.AddUser.TIME_SERVER;

public class UserHomePage extends AppCompatActivity implements View.OnClickListener{

    ImageButton logout_ib;
    FancyButton sendMoney_btn;
    TextView uid_tv, totalBalance_tv, transaction_tv, amountInWords_tv;

    EditText amountToBeSent_et, userUIDToSendMoney_et;

    SharedPreferences userIdentifierSharedPreferences;
    private static final String USER_IDENTIFIER_PREF = "userIdentifierPref";
    private static final String USER_UID = "user_uid";
    private static final String USER_KEY = "user_key";

    private static final String LANDING_ACTIVITY = "landingActivity";
    private static final String FIRST_SCREEN = "firstScreen";
    private static final String USER_BALANCE = "userBalance";
    private static final String TOTAL_BALANCE = "total_balance";

    String userUID_tx, amountToBeSent_tx, userUIDToSendMoney_tx, userKeyToSendMoney_tx, today_dateTime;

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    FirebaseAuth mAuth;
    Boolean checkUser;
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home_page);
        mAuth = FirebaseAuth.getInstance();

        logout_ib = findViewById(R.id.auh_logoutButton);
        sendMoney_btn = findViewById(R.id.auh_sendMoneyButton);
        uid_tv = findViewById(R.id.auh_userUIDTextView);

        totalBalance_tv = findViewById(R.id.auh_totalBalanceTextView);
        transaction_tv = findViewById(R.id.auh_transaction);
        amountToBeSent_et = findViewById(R.id.auh_amountEditText);
        userUIDToSendMoney_et = findViewById(R.id.auh_userUIDEditText);
        amountInWords_tv = findViewById(R.id.auh_numberToEnglishTextView);

        progressDialog = new ProgressDialog(UserHomePage.this);
        progressDialog.setMessage("Please Wait|Making Transaction...");

//        text watcher on amount
        amountToBeSent_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String return_val_in_english =   EnglishNumberToWords.convert(Long.parseLong(s.toString()));
                    amountInWords_tv.setText(return_val_in_english+".");
                }
                catch (NumberFormatException e){
                    e.printStackTrace();
                }
                amountToBeSent_et.setBackgroundColor(getResources().getColor(R.color.cyanLight));

                if (start == 0) {
                    amountInWords_tv.setText("");
                    amountToBeSent_et.setBackgroundColor(getResources().getColor(R.color.wheat));
                }
                if (count == 1) amountToBeSent_et.setBackgroundColor(getResources().getColor(R.color.cyanLight));
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //        text watcher on uid
        userUIDToSendMoney_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start == 6){
                    userUIDToSendMoney_et.setBackgroundColor(getResources().getColor(R.color.cyanLight));
                }
                else userUIDToSendMoney_et.setBackgroundColor(getResources().getColor(R.color.wheat));
                if (count == 0) userUIDToSendMoney_et.setBackgroundColor(getResources().getColor(R.color.wheat));
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (start == 6){
                    userUIDToSendMoney_et.setBackgroundColor(getResources().getColor(R.color.cyanLight));
                }
                else userUIDToSendMoney_et.setBackgroundColor(getResources().getColor(R.color.wheat));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        logout_ib.setOnClickListener(this);
        transaction_tv.setOnClickListener(this);
        sendMoney_btn.setOnClickListener(this);
    }

//    onStart
    public void onStart(){
        super.onStart();

        userIdentifierSharedPreferences = getSharedPreferences(USER_IDENTIFIER_PREF, MODE_PRIVATE);
        userUID_tx = userIdentifierSharedPreferences.getString(USER_UID, "");
        uid_tv.setText("UID: "+userUID_tx);
        setTotalBalance();
    }
//    onStart

//    onclick
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.auh_logoutButton: //logout
                Logout();
                break;
            case R.id.auh_transaction: // load transaction fragment
                LoadTransactions();
                break;
            case R.id.auh_sendMoneyButton: // send money to other uid
                SendMoney();
                break;
        }

    }//onClick

//    send money
    private void SendMoney() {
        userUIDToSendMoney_tx = userUIDToSendMoney_et.getText().toString().trim();
        amountToBeSent_tx = amountToBeSent_et.getText().toString().trim();
        if (Validations()) { //if 1
            if (CheckIfSendingToItSelf()){//if 2
                new MaterialDialog.Builder(UserHomePage.this)
                        .title("Are you sure to Make this Transaction!")
                        .content("Rs "+amountToBeSent_tx+"/- will be credited to the Account with UID:"+userUIDToSendMoney_tx)
                        .contentColorRes(R.color.white)
                        .titleColor(getResources().getColor(R.color.whiteSmoke))
                        .positiveText("Confirm")
                        .positiveColorRes(R.color.googleGreen)
                        .negativeText("Cancel")
                        .negativeColorRes(R.color.googleRed)
                        .backgroundColor(getResources().getColor(R.color.black90))
                        .icon(getResources().getDrawable(R.drawable.ic_transfer_money))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                progressDialog.show();
                                CheckIfUserExist();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }//if 2
        }//if 1

    }

//    check if sending money to itself
    private Boolean CheckIfSendingToItSelf() {
        userIdentifierSharedPreferences = getSharedPreferences(USER_IDENTIFIER_PREF, MODE_PRIVATE);
        userUID_tx = userIdentifierSharedPreferences.getString(USER_UID, "");
        if (TextUtils.equals(userUID_tx, userUIDToSendMoney_tx)){
                new MaterialDialog.Builder(UserHomePage.this)
                        .title("Transaction Failed!")
                        .titleColor(Color.WHITE)
                        .content("You can not Transact Balance To Yourself.\nTry Transaction with another account.")
                        .icon(getResources().getDrawable(R.drawable.ic_warning))
                        .contentColor(getResources().getColor(R.color.lightCoral))
                        .backgroundColor(getResources().getColor(R.color.black90))
                        .positiveText(R.string.ok)
                        .show();
                return false;
        }
        return true;
    }
    //    check if sending money to itself

    //    checking user
    private void CheckIfUserExist() {
        checkUser = false;
        databaseReference.child("userProfile")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()){ // for
                            final String uid = snapshot.child("uid").getValue(String.class);
                            if (TextUtils.equals(uid, userUIDToSendMoney_tx)) {  // if user exist
                                userKeyToSendMoney_tx = snapshot.child("key").getValue(String.class);
                                checkUser = true;
//                                making transaction
                                CheckIfSufficientBalanceIsAvailable();
                                return;
                            }
                        }//for

                         //if user does not exist
                        if (!checkUser){
                            new MaterialDialog.Builder(UserHomePage.this)
                                    .title("No Such User Exist!")
                                    .titleColor(Color.WHITE)
                                    .content("User with " +
                                            "UID: "+userUIDToSendMoney_tx
                                            +" does not exist. Please re-check the UID.")
                                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                                    .contentColor(getResources().getColor(R.color.lightCoral))
                                    .backgroundColor(getResources().getColor(R.color.black90))
                                    .positiveText(R.string.ok)
                                    .show();
                            progressDialog.dismiss();
                        }
                        progressDialog.dismiss();
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        new MaterialDialog.Builder(UserHomePage.this)
                                .title(databaseError.toString())
                                .titleColor(Color.WHITE)
                                .icon(getResources().getDrawable(R.drawable.ic_warning))
                                .contentColor(getResources().getColor(R.color.lightCoral))
                                .backgroundColor(getResources().getColor(R.color.black90))
                                .positiveText(R.string.ok)
                                .show();
                        progressDialog.dismiss();
                    }
                });//database refer
    }
    //    checking user

//    checking is balance is available
    private void CheckIfSufficientBalanceIsAvailable() {
        final String key = mAuth.getCurrentUser().getUid();
        databaseReference.child("userBalance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String balance = dataSnapshot.child(key).child("total_balance").getValue(String.class);
                        final int d_balance = Integer.parseInt(balance);
                        final int d_balanceToBeSent = Integer.parseInt(amountToBeSent_tx);
                        if (d_balance < d_balanceToBeSent){
                            new MaterialDialog.Builder(UserHomePage.this)
                                    .title("Transaction Failed")
                                    .titleColor(Color.WHITE)
                                    .content("You Do not have sufficient balance to made this transaction. " +
                                            "Contact Your Aangadia to add balance.")
                                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                                    .contentColor(getResources().getColor(R.color.lightCoral))
                                    .backgroundColor(getResources().getColor(R.color.black90))
                                    .positiveText(R.string.ok)
                                    .show();
                            progressDialog.dismiss();
                        }
//                        make transaction
                        else { //else
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Date dateTime = null;
                                    try {
                                        NTPUDPClient timeClient = new NTPUDPClient();
                                        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
                                        TimeInfo timeInfo = timeClient.getTime(inetAddress);
                                        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
                                        dateTime = new Date(returnTime);
                                    }
                                    catch (IOException e){
                                        Log.w("raky", e.getCause());
                                    }
                                    Log.w("raky", String.valueOf(dateTime));
                                    String myDate = String.valueOf(dateTime);
                                    String date, time, year, month;
                                    date = myDate.substring(8, 10);
                                    month = myDate.substring(4, 7);
                                    time = myDate.substring(11, 16);
                                    year = myDate.substring(myDate.length()-4, myDate.length());
                                    today_dateTime = time+", "+date+" "+month+" "+year;
                                    MakeTransaction();

                                }
                            });
                            thread.start();

                        }//else
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        new MaterialDialog.Builder(UserHomePage.this)
                                .title(databaseError.toString())
                                .titleColor(Color.WHITE)
                                .icon(getResources().getDrawable(R.drawable.ic_warning))
                                .contentColor(getResources().getColor(R.color.lightCoral))
                                .backgroundColor(getResources().getColor(R.color.black90))
                                .positiveText(R.string.ok)
                                .show();
                        progressDialog.dismiss();
                    }
                });
    }
//no sufficient balance


//    finally Making Transaction
    private void MakeTransaction() {
//     getting balance of user to whom balance is to be sent
        databaseReference.child("userBalance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        getting sender balance
                        String myKey = mAuth.getCurrentUser().getUid();
                        String sender_balance = dataSnapshot.child(myKey).child("total_balance")
                                .getValue(String.class);
                        final int my_deducted_balance = Integer.parseInt(sender_balance) - Integer.parseInt(amountToBeSent_tx);
//                        setting my deducted balance
                        databaseReference.child("userBalance").child(myKey).child("total_balance")
                                .setValue(String.valueOf(my_deducted_balance));
//                         adding amount to receiver account
                        String receiver_balance = dataSnapshot.child(userKeyToSendMoney_tx).child("total_balance")
                                .getValue(String.class);
                        final int updated_balance_of_receiving_user = Integer.parseInt(receiver_balance) +
                                Integer.parseInt(amountToBeSent_tx);
                        databaseReference.child("userBalance").child(userKeyToSendMoney_tx).child("total_balance")
                                .setValue(String.valueOf(updated_balance_of_receiving_user));

                        final String push_key = databaseReference.push().getKey();

//                        setting sender transaction
                        DatabaseReference databaseReferenceSender = databaseReference.child("transactions").child(myKey).child(push_key);
                        databaseReferenceSender.child("mode").setValue("debit");
                        databaseReferenceSender.child("balance_debited").setValue(amountToBeSent_tx);
                        databaseReferenceSender.child("current_balance").setValue(sender_balance);
                        databaseReferenceSender.child("receiver_key").setValue(userKeyToSendMoney_tx);
                        databaseReferenceSender.child("balance_after_debit").setValue(my_deducted_balance);
                        databaseReferenceSender.child("dateTime").setValue(today_dateTime);

//                        setting receiver transaction
                        DatabaseReference databaseReferenceReceiver = databaseReference.child("transactions").child(userKeyToSendMoney_tx).child(push_key);
                        databaseReferenceReceiver.child("mode").setValue("credit");
                        databaseReferenceReceiver.child("balance_credited").setValue(amountToBeSent_tx);
                        databaseReferenceReceiver.child("current_balance").setValue(receiver_balance);
                        databaseReferenceReceiver.child("sender_key").setValue(myKey);
                        databaseReferenceReceiver.child("balance_after_credit").setValue(updated_balance_of_receiving_user);
                        databaseReferenceReceiver.child("dateTime").setValue(today_dateTime);

                        progressDialog.dismiss();

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
    //    finally Making Transaction


//    send money

//    validations
    public Boolean Validations(){
        if (userUIDToSendMoney_et.length() != 7){
            new MaterialDialog.Builder(UserHomePage.this)
                    .title("Not a Valid User UID")
                    .titleColor(Color.WHITE)
                    .content("UID is 7 in length")
                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                    .contentColor(getResources().getColor(R.color.lightCoral))
                    .backgroundColor(getResources().getColor(R.color.black90))
                    .positiveText(R.string.ok)
                    .show();
            return false;
        }
        else if (TextUtils.isEmpty(amountToBeSent_tx)){
            new MaterialDialog.Builder(UserHomePage.this)
                    .title("Failed")
                    .titleColor(Color.WHITE)
                    .content("Please Enter Amount")
                    .icon(getResources().getDrawable(R.drawable.ic_warning))
                    .contentColor(getResources().getColor(R.color.lightCoral))
                    .backgroundColor(getResources().getColor(R.color.black90))
                    .positiveText(R.string.ok)
                    .show();
            return false;
        }
        return true;
    }
//  validations

    //    Load Transactions
    private void LoadTransactions() {
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container_user_home_page, new UserTransactions()).addToBackStack("transactions").commit();
    }
//    Load Transactions


//    setBalance
//    setting total balance
private void setTotalBalance() {
    final String key = userIdentifierSharedPreferences.getString(USER_KEY, "");
    databaseReference.child(USER_BALANCE).child(key)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final String total_balance = dataSnapshot.child(TOTAL_BALANCE).getValue(String.class);
                    if (!TextUtils.isEmpty(total_balance)){
                        NumberFormat formatter = new DecimalFormat("#,###");
                        String formatted_balance = formatter.format(Long.parseLong(total_balance));
                        totalBalance_tv.setText(formatted_balance);
                    }
                    else {
                        totalBalance_tv.setText("0.00");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
}
//set balance


//    logout
    private void Logout() {
        new MaterialDialog.Builder(this)
                .title("Logout")
                .content("Are You Sure to Logout?")
                .positiveText("Yes")
                .positiveColor(getResources().getColor(R.color.googleRed))
                .negativeText("No")
                .icon(getResources().getDrawable(R.drawable.ic_logout))
                .backgroundColor(getResources().getColor(R.color.black90))
                .negativeColor(getResources().getColor(R.color.googleGreen))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        SharedPreferences sharedpreferences = getSharedPreferences(LANDING_ACTIVITY, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(FIRST_SCREEN, "");
                        editor.apply();

                        mAuth = FirebaseAuth.getInstance();
                        mAuth.signOut();

                        UserHomePage.this.finish();
                        startActivity(new Intent(UserHomePage.this, Login.class));

                        // TODO
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        // TODO
                    }
                }) .onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                // TODO
            }
        })
                .show();
    }
//    logout

    //ends
}



