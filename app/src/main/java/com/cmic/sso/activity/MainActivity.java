package com.cmic.sso.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cmic.sso.R;
import com.cmic.sso.sdk.auth.AuthnHelper;
import com.cmic.sso.sdk.auth.TokenListener;
import com.cmic.sso.sdk.utils.rglistener.RegistListener;
import com.cmic.sso.tokenValidate.Request;
import com.cmic.sso.tokenValidate.RequestCallback;
import com.cmic.sso.util.Constant;
import com.cmic.sso.util.SharePersist;
import com.cmic.sso.util.StringFormat;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


@TargetApi(23)
@SuppressLint("HandlerLeak")
public class MainActivity extends Activity implements View.OnClickListener {

    protected String TAG = "MainActivity";
    private static final int RESULT = 0x111;
    protected static final int RESULTOFSIMINFO = 0x222;
    private Context mContext;
    private String mResultString;
    private TextView mResultText;
    private TextView mVersionText;
    private TextView phoneEt;
    private String mSDKVersion=null;
    private TokenListener mListener;
    private EditText mMobileEdit;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE_PRE = 1000;
    private static final int PERMISSIONS_REQUEST_READ_PHONE_STATE_IMPLICITLOGIN = 2000;


    /**
     * 获取用户资料所需要的信息，可以用shareprefence保存，同时返回的还有有效期，注意处理
     */
    private String mAccessToken;
    private String mUniqueId;
    private AuthnHelper mAuthnHelper;
    private RegistListener registListener;
    /**
     * 是否使用测试线
     */
    private boolean isTest = false;

    private ResultDialog mResultDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mAuthnHelper = AuthnHelper.getInstance(mContext.getApplicationContext());
        mAuthnHelper.setTimeOut(8000);
        mSDKVersion=mAuthnHelper.SDK_VERSION;
        init();
        mAuthnHelper.setDebugMode(true);
    }


    private void init() {
        setContentView(R.layout.activity_main);
        findViewById(R.id.wap_login1).setOnClickListener(this);
        findViewById(R.id.sms_login).setOnClickListener(this);
        findViewById(R.id.outh_login).setOnClickListener(this);
        findViewById(R.id.get_user_info).setOnClickListener(this);
        findViewById(R.id.logout_Authorization).setOnClickListener(this);
        findViewById(R.id.mobilenumberStatus).setOnClickListener(this);
        findViewById(R.id.verifyMobile).setOnClickListener(this);
        findViewById(R.id.SimStatus).setOnClickListener(this);
        findViewById(R.id.search_account).setOnClickListener(this);
        findViewById(R.id.pre_getphone).setOnClickListener(this);
        findViewById(R.id.validate_phone_bt).setOnClickListener(this);
        phoneEt = (TextView) findViewById(R.id.phone_et);
        mVersionText= (TextView) findViewById(R.id.text_version);
        mVersionText.setText(mSDKVersion);
        mResultDialog = new ResultDialog(mContext);

        mResultText = (TextView) findViewById(R.id.text_result);
        mListener = new TokenListener() {
            @Override
            public void onGetTokenComplete(JSONObject jObj) {
                if (jObj != null) {
                    try {
                        //时间
                        long prePhoneTimes = SharePersist.getInstance().getLong(mContext, "getPrePhoneTimes");
                        long phoneTimes = SharePersist.getInstance().getLong(mContext, "phonetimes");
                        if(prePhoneTimes == 0){
                            jObj.put("phonetimes", System.currentTimeMillis() -  phoneTimes + "");
                        }else if(phoneTimes == 0){
                            jObj.put("getPrePhoneTimes", System.currentTimeMillis() - prePhoneTimes + "");
                        }
                        mResultString = jObj.toString();
                        mHandler.sendEmptyMessage(RESULT);
                        if (jObj.has("token")) {
                            mAccessToken = jObj.optString("token");
                            HashMap<String, String> map = new HashMap<>(2);
                            map.put("token", mAccessToken);
                            MobclickAgent.onEvent(MainActivity.this, "user_token", map);
                        }
                        MobclickAgent.onEvent(MainActivity.this, "getResult", jObj.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

    }

    @Override
    public void onClick(View v) {
//        LogUtils.e("开始请求sdk验证:", "开始请求sdk鉴权:");
        mResultText.setText("开始请求...");
        switch (v.getId()) {
            case R.id.sms_login:
                displayLogin();
                break;
            case R.id.get_user_info:
                getUserInfo();
                break;
            case R.id.pre_getphone:
                preGetPhone();
                break;
            case R.id.wap_login1:
                implicitLogin();
                break;
            case R.id.validate_phone_bt:
                phoneValidate();
                break;
            default:
                break;
        }
    }

    private void getUserInfo() {
        tokenValidate(Constant.APP_ID, Constant.APP_KEY, mAccessToken, mListener);
    }

    public void tokenValidate(final String appId, final String appKey, final String token, final TokenListener listener) {
        Bundle values = new Bundle();
        values.putString("appkey", appKey);
        values.putString("appid", appId);
        values.putString("token", token);
        Request.getInstance(mContext).tokenValidate(values, new RequestCallback() {
            @Override
            public void onRequestComplete(String resultCode, String resultDes, JSONObject jsonobj) {
                Log.i("Token校验结果：", jsonobj.toString());
                listener.onGetTokenComplete(jsonobj);
                String phone = jsonobj.optString("msisdn");
                HashMap<String, String> map = new HashMap<>(2);
                map.put("token", mAccessToken);
                map.put("msisdn", phone);
                MobclickAgent.onEvent(MainActivity.this, "user_phone", map);
                MobclickAgent.onEvent(MainActivity.this, "tokenValidateResult", jsonobj.toString());
            }
        });
    }

    private void phoneValidate(){
        phoneValidate(Constant.APP_ID, Constant.APP_KEY, mAccessToken, mListener);
    }

    public void phoneValidate(final String appId, final String appKey, final String token, final TokenListener listener) {
        Bundle values = new Bundle();
        values.putString("appkey", appKey);
        values.putString("appid", appId);
        values.putString("token", token);
        values.putString("phone", phoneEt.getText().toString());
        Request.getInstance(mContext).phoneValidate(values, new RequestCallback() {
            @Override
            public void onRequestComplete(String resultCode, String resultDes, JSONObject jsonobj) {
                Log.i("Token校验结果：", jsonobj.toString());
                listener.onGetTokenComplete(jsonobj);
            }
        });
    }


    private void preGetPhone(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSIONS_REQUEST_READ_PHONE_STATE_PRE);
            }else {
                SharePersist.getInstance().putLong(mContext, "getPrePhoneTimes", System.currentTimeMillis());
                SharePersist.getInstance().putLong(mContext, "phonetimes", 0);
                mAuthnHelper.umcLoginPre(Constant.APP_ID, Constant.APP_KEY, mListener);
            }
        }else {
            SharePersist.getInstance().putLong(mContext, "getPrePhoneTimes", System.currentTimeMillis());
            SharePersist.getInstance().putLong(mContext, "phonetimes", 0);
            mAuthnHelper.umcLoginPre(Constant.APP_ID, Constant.APP_KEY, mListener);
        }
    }

  /*
    public static final String AUTH_TYPE_USER_PASSWD = "1";//用户名密码
    public static final String AUTH_TYPE_DYNAMIC_SMS = "2";//短信验证码
    public static final String AUTH_TYPE_WAP = "3";//网关鉴权
    public static final String AUTH_TYPE_SMS = "4";//短信上行
    */
    private void displayLogin() {
        SharePersist.getInstance().putLong(mContext, "getPrePhoneTimes", 0);
        SharePersist.getInstance().putLong(mContext, "phonetimes", System.currentTimeMillis());
        mAuthnHelper.getTokenExp(Constant.APP_ID, Constant.APP_KEY,
                 AuthnHelper.AUTH_TYPE_DYNAMIC_SMS + AuthnHelper.AUTH_TYPE_SMS, mListener);
    }
    private void implicitLogin() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                        PERMISSIONS_REQUEST_READ_PHONE_STATE_IMPLICITLOGIN);
            }else {
                mAuthnHelper.getTokenImp(Constant.APP_ID, Constant.APP_KEY,mListener);
            }
        }else {
            mAuthnHelper.getTokenImp(Constant.APP_ID, Constant.APP_KEY,mListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_PHONE_STATE_PRE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    preGetPhone();
                } else {
                    mListener.onGetTokenComplete(StringFormat.getLoginResult("200005", "用户未授权READ_PHONE_STATE"));
                }
                break;
            case PERMISSIONS_REQUEST_READ_PHONE_STATE_IMPLICITLOGIN:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    implicitLogin();
                } else {
                    mListener.onGetTokenComplete(StringFormat.getLoginResult("200005", "用户未授权READ_PHONE_STATE"));
                }
                break;
            default:
                break;
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RESULT:
                    mResultText.setText(mResultString);
                    mResultDialog.setResult(StringFormat
                            .logcatFormat(mResultString));
                    break;
                case RESULTOFSIMINFO:
                    mResultText.setText(mResultString);
                    mResultDialog.setResult(mResultString);
                default:
                    break;
            }
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
