package jp.co.toshibatec.searchsampletool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.co.sharedmodule.BluetoothConnectionService;
import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.ConnectionEventHandler;
import jp.co.toshibatec.searchsampletool.log.Log;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends LibAccessBaseActivity implements View.OnClickListener,CompoundButton.OnCheckedChangeListener {
    FirebaseFirestore firestore;
    private static MainActivity mMainActivity = null;
    /** 探索ボタン */
    private Button mSearchBtn = null;

    private Button mProductCodeCheckBtn = null;
    /** 探索除外対象EPC */
    private EditText mExclusionEditText = null;
    /** 探索対象EPC */
    private EditText mSearchEditText = null;
    /** 探索対象リスト追加ボタン */
    private Button mEpcListAddBtn = null;
    /** 探索対象リストクリアボタン */
    private Button mEpcListClearBtn = null;
    /** 探索除外対象リスト追加ボタン */
    private Button mExclusionListAddBtn = null;
    /** 探索除外対象リストクリアボタン */
    private Button mExclusionListClearBtn = null;
    /** 探索対象EPC指示ラベル */
    private TextView mSearchText  = null;
    /** 探索除外対象指示ラベル */
    private TextView mExclusionText = null;
    /** 探索対象リスト用テーブル */
    private TableLayout mSearchTableLayout  = null;
    /** 探索除外対象リスト用テーブル */
    private TableLayout mExclusionTableLayout = null;
    /** 探索タイプ選択スイッチ */
    private Switch mSearhSelectSwitch = null;
    /** EPCリスト用レイアウト */
    private LinearLayout mEpcListLayout = null;
    /** 除外リスト用レイアウト */
    private LinearLayout mExclusionLayout = null;
    /** Bluetoothアドレスエディットボックス */
    private EditText mBluetoothAddr = null;
    /** 接続ボタン */
    private Button mConnect = null;
    /** 機種選択スイッチ */
    private Switch mSwitchType = null;

    /** ログ出力用 */
    private static final int WRITE_TO_CONSOLE_AND_SD = 0;
    /** SDK出力ログレベル */
    private int mSDKLogLevel = TecRfidSuite.LOG_LEVEL_INFO;
    /** SDK出力ログサイズ */
    private int mSDKLogSize = DEFAULT_LOG_SIZE;
    /** インターフェイス */
    private static NotifyForActivityInterface mSettingTool = null;
    /** ClaimDevice用スレッド */
    private Thread mClaimDeviceThread = null;
    /** ClaimDevice用スレッド */
    private Runnable mClaimDeviceRunnable = null;
    /** ログサイズ(デフォルト) */
    public static final int DEFAULT_LOG_SIZE = 1024 * 10;
    /** 探索対象(EPC) */
    private String searchTarget = null;
    /** EPC指定済み */
    private boolean isSelectedEPC = true;
    /** 探索対象指定リスト */
    private ArrayList<String> mEpcCodeList = new ArrayList<String>();
    /** 除外対象リスト */
    private ArrayList<String> mExclusionList = new ArrayList<String>();
    /**
     * claimDevice用引数
     *
     * 接続済みMACアドレス */
    private String mConnectedString = null;

    /**
     * open用引数
     *
     * deviceName */
    private static final String DEVICENAME = "UF-2200";
    private static final String DEVICENAME_UF3000 = "UF-3000";
    /** 切断検知フラグ */
    private boolean mDisconnectFlag = false;
    /** 再接続用 */
    private String mReConnectString = null;

    /** 設定ファイル保存ディレクトリパス */
    private static final String SETTING_PATH = "/TEC/SearchSample/";
    /** ファイル名 */
    private static final String SETTING_FILENAME = "Setting.txt";
    /** テキスト書込み用 */
    private static final String COMMA = ",";
    /** 探索モードFW設定用KEY */
    public final static String KEY_FW_MODE = "FwMode";
    /** 探索モードFW設定(デフォルト) */
    public static final int DEFAULT_FW_MODE = 1;
    /** 描画モード設定用KEY */
    public final static String KEY_RADAR_DRAW_MODE = "Radar_DrawMode";
    /** 描画モード設定(デフォルト) */
    public static final int DEFAULT_RADAR_DRAW_MODE = 1;
    /** 改行 */
    private static final String NEWLINE = "\n";
    /** 機種判定用 */
    private boolean isUF3000 = true;

    private EditText mProductCode = null;

    private String mConnectionRequestString = null;
    private String source = null;


    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        readInitSettingFile();
        if(getActionBar()!=null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        if (getIntent().hasExtra(KEY_TARGET)) {
            searchTarget = getIntent().getStringExtra(KEY_TARGET);
            android.util.Log.d("parentChildrenMap", searchTarget.toString());
        }
        if (getIntent().hasExtra("EXTRA_CONNECTION_REQUEST")) {
            mConnectionRequestString = getIntent().getStringExtra("EXTRA_CONNECTION_REQUEST");
        }
        if (getIntent().hasExtra("SOURCE")) {
            source = getIntent().getStringExtra("SOURCE");
        }
        this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);
        // ログサイズ設定
        Log.setMaxFileSize(mSDKLogSize);
        Log.setLogOutPut(WRITE_TO_CONSOLE_AND_SD);
        Log.setNowLevel(mSDKLogLevel);
        // 探索開始ボタン設定
        Object omSearchBtn = findViewById(R.id.search_btn);
        if (omSearchBtn instanceof Button) {
            mSearchBtn = (Button) omSearchBtn;
        } else {
            mSearchBtn = new Button(MainActivity.this);
        }
        mSearchBtn.setOnClickListener(this);
        Object omProductCodeCheckBtn = findViewById(R.id.productcode_check_button);
        if (omProductCodeCheckBtn instanceof Button) {
            mProductCodeCheckBtn = (Button) omProductCodeCheckBtn;
        } else {
            mProductCodeCheckBtn = new Button(MainActivity.this);
        }
        mProductCodeCheckBtn.setOnClickListener(this);
        // 探索対象リスト追加ボタン設定
        Object omEpcListAddBtn = findViewById(R.id.epclist_add_button);
        if (omEpcListAddBtn instanceof Button) {
            mEpcListAddBtn = (Button) omEpcListAddBtn;
        } else {
            mEpcListAddBtn = new Button(MainActivity.this);
        }
        mEpcListAddBtn.setOnClickListener(this);
        // 探索対象リストクリアボタン設定
        Object omEpcListClearBtn = findViewById(R.id.epclist_clear_button);
        if (omEpcListClearBtn instanceof Button) {
            mEpcListClearBtn = (Button) omEpcListClearBtn;
        } else {
            mEpcListClearBtn = new Button(MainActivity.this);
        }
        mEpcListClearBtn.setOnClickListener(this);
        // 探索除外対象リスト追加ボタン設定
        Object omExclusionListAddBtn = findViewById(R.id.exclusion_add_button);
        if (omExclusionListAddBtn instanceof Button) {
            mExclusionListAddBtn = (Button) omExclusionListAddBtn;
        } else {
            mExclusionListAddBtn = new Button(MainActivity.this);
        }
        mExclusionListAddBtn.setOnClickListener(this);
        // 探索除外対象リストクリアボタン設定
        Object omExclusionListClearBtn = findViewById(R.id.exclusion_clear_button);
        if (omExclusionListClearBtn instanceof Button) {
            mExclusionListClearBtn = (Button) omExclusionListClearBtn;
        } else {
            mExclusionListClearBtn = new Button(MainActivity.this);
        }
        mExclusionListClearBtn.setOnClickListener(this);

        // 探索対象EPC設定
        Object omSearchEditText = findViewById(R.id.target_text);
        if (omSearchEditText instanceof EditText) {
            mSearchEditText = (EditText) omSearchEditText;
        } else {
            mSearchEditText = new EditText(MainActivity.this);
        }
        if (searchTarget != null) {
            mSearchEditText.setText(searchTarget);
        }
        // 探索除外対象EPC設定
        Object omExclusionEditText = findViewById(R.id.exclusion_text);
        if (omExclusionEditText instanceof EditText) {
            mExclusionEditText = (EditText) omExclusionEditText;
        } else {
            mExclusionEditText = new EditText(MainActivity.this);
        }
        // 探索対象EPC指示ラベル設定
        Object omSearchText = findViewById(R.id.target_label);
        if (omSearchText instanceof TextView) {
            mSearchText = (TextView) omSearchText;
        } else {
            mSearchText = new TextView(MainActivity.this);
        }
        // 探索除外対象指示ラベル設定
        Object omExclusionText = findViewById(R.id.exclusion_label);
        if (omExclusionText instanceof TextView) {
            mExclusionText = (TextView) omExclusionText;
        } else {
            mExclusionText = new TextView(MainActivity.this);
        }
        // 探索対象リスト用テーブル設定
        Object omSearchTableLayout = findViewById(R.id.target_TableLayout);
        if (omSearchTableLayout instanceof TableLayout) {
            mSearchTableLayout = (TableLayout) omSearchTableLayout;
        } else {
            mSearchTableLayout = new TableLayout(MainActivity.this);
        }
        // 探索除外対象リスト用テーブル設定
        Object omExclusionTableLayout = findViewById(R.id.exclusion_TableLayout);
        if (omExclusionTableLayout instanceof TableLayout) {
            mExclusionTableLayout = (TableLayout) omExclusionTableLayout;
        } else {
            mExclusionTableLayout = new TableLayout(MainActivity.this);
        }
        // EPCリスト用レイアウト設定
        Object omEpcListLayout = findViewById(R.id.target_list_layout);
        if (omEpcListLayout instanceof LinearLayout) {
            mEpcListLayout = (LinearLayout) omEpcListLayout;
        } else {
            mEpcListLayout = new LinearLayout(MainActivity.this);
        }
        // 除外リスト用レイアウト設定
        Object omExclusionLayout = findViewById(R.id.exclusion_list_layout);
        if (omExclusionLayout instanceof LinearLayout) {
            mExclusionLayout = (LinearLayout) omExclusionLayout;
        } else {
            mExclusionLayout = new LinearLayout(MainActivity.this);
        }
        // 探索タイプ選択スイッチ設定
        Object omSearhSelectSwitch = findViewById(R.id.search_switch);
        if (omSearhSelectSwitch instanceof Switch) {
            mSearhSelectSwitch = (Switch) omSearhSelectSwitch;
        } else {
            mSearhSelectSwitch = new Switch(MainActivity.this);
        }
        mSearhSelectSwitch.setOnCheckedChangeListener(this);
        // Bluetoothアドレスエディットボックス設定
        Object omBluetoothAddr = findViewById(R.id.editTextBluetoothAddr);
        if (omBluetoothAddr instanceof EditText) {
            mBluetoothAddr = (EditText) omBluetoothAddr;
        } else {
            mBluetoothAddr = new EditText(MainActivity.this);
        }
        // 接続ボタン設定
        Object omConnect = findViewById(R.id.buttonConnect);
        if (omConnect instanceof Button) {
            mConnect = (Button) omConnect;
        } else {
            mConnect = new Button(MainActivity.this);
        }
        mConnect.setOnClickListener(this);

        // 機種選択スイッチ設定
        Object omSwitchType = findViewById(R.id.switchType);
        if (omSwitchType instanceof Switch) {
            mSwitchType = (Switch) omSwitchType;
        } else {
            mSwitchType = new Switch(MainActivity.this);
        }
        mSwitchType.setOnCheckedChangeListener(this);

        mMainActivity = this;
        Log.info(END);
    }

    private static boolean isFirst = true;
    @Override
    protected void onResume() {
        Log.info(START);
        super.onResume();
        if(isFirst) {
            isFirst = false;
            int result = open(DEVICENAME_UF3000, MainActivity.this, mSDKLogLevel, mSDKLogSize);
            if (result == TecRfidSuite.OPOS_SUCCESS) {
                close();
            }
        }
        Log.info(END);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent();
            if (source != null && source.equals("child")) {
                intent.setClassName(this, "jp.co.toshibatec.uf2200sampleapplication.PartsFinderChildActivity");
            }
            else {
                intent.setClassName(this, "jp.co.toshibatec.uf2200sampleapplication.MainMenuActivity");
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        Log.info(START);
        if (view.equals(mSearchBtn)) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            if(isSelectedEPC) {
                searchTarget = mSearchEditText.getText().toString();
                if(searchTarget.length()==0) {
                    showDialog(getString(R.string.title_error), getString(R.string.message_target_not_set_error), getString(R.string.btn_txt_ok), null);
                    return;
                }
                intent.putExtra(KEY_TARGET, searchTarget);
                intent.putExtra(KEY_SELECTED_EPC, isSelectedEPC);
            }
            else {
                if(mEpcCodeList.size()==0) {
                    showDialog(getString(R.string.title_error), getString(R.string.message_target_not_set_error), getString(R.string.btn_txt_ok), null);
                    return;
                }
                intent.putExtra(KEY_EPCLIST, mEpcCodeList);
                intent.putExtra(KEY_EXCLUSIONLIST, mExclusionList);
                intent.putExtra(KEY_SELECTED_EPC, mExclusionList);
            }
            startActivity(intent);
        }
        if (view.equals(mProductCodeCheckBtn)) {
            firestore = FirebaseFirestore.getInstance();
            mProductCode = findViewById(R.id.product_code_text);
            final String productCode = mProductCode.getText().toString();
            firestore.collection("tag_list")
                    .whereEqualTo("product_code", productCode)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            CollectionReference tag_list = firestore.collection("tag_list");
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    Toast.makeText(getApplicationContext(), "Product Code is not registered", Toast.LENGTH_LONG).show();
                                } else {
                                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                    String epcCode = document.getString("epc_code");
                                    mSearchEditText.setText(epcCode);
                                    Toast.makeText(getApplicationContext(), "Product Code is registered", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Error checking tag", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
        else if(view.equals(mEpcListAddBtn)) {
            if(mSearchEditText.getText()==null || mSearchEditText.length()==0) {
                return;
            }
            if(mEpcCodeList.size()>8) {
                showDialog(getString(R.string.title_error), getString(R.string.message_target_setting_error), getString(R.string.btn_txt_ok), null);
                return;
            }
            String target = mSearchEditText.getText().toString();
            TableRow tableRow = new TableRow(this);
            TextView text= new TextView(this);
            text.setText(target);
            tableRow.addView(text);
            mSearchTableLayout.addView(tableRow);
            mEpcCodeList.add(target);
            mSearchEditText.setText("");
        }
        else if(view.equals(mEpcListClearBtn)) {
            mEpcCodeList.clear();
            mSearchTableLayout.removeAllViews();
        }
        else if(view.equals(mExclusionListAddBtn)) {
            if(mExclusionEditText.getText()==null || mExclusionEditText.length()==0) {
                return;
            }
            String target = mExclusionEditText.getText().toString();
            TableRow tableRow = new TableRow(this);
            TextView text= new TextView(this);
            text.setText(target);
            tableRow.addView(text);
            mExclusionTableLayout.addView(tableRow);
            mExclusionList.add(target);
            mExclusionEditText.setText("");
        }
        else if(view.equals(mExclusionListClearBtn)) {
            mExclusionList.clear();
            mExclusionTableLayout.removeAllViews();
        }
        else if(view.equals(mConnect)) {
            Log.info(START);
            setListener(null);
            String connectedAddress = mConnectedString;
            final String connectRequestAddress = mBluetoothAddr.getText().toString();

            // 接続要求デバイスがあれば
            if (null != connectRequestAddress) {
                // 接続済みデバイスがある
                if (null != connectedAddress) {
                    // 違うデバイスに接続したい
                    if (!connectRequestAddress.equals(connectedAddress)) {
                        // デバイス切断
                        deviceDisConnect();
                        // デバイス接続
                        deviceConnect(connectRequestAddress);
                    }
                } else {
                    // デバイス接続
                    deviceConnect(connectRequestAddress);
                }
            }
            Log.info(END);
        }
        Log.info(END);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.info(START);
        if (buttonView.equals(mSearhSelectSwitch)) {
            if(isChecked) {
                mSearchText.setText(getText(R.string.search_list_description_text));
                mEpcListAddBtn.setVisibility(View.VISIBLE);
                mEpcListClearBtn.setVisibility(View.VISIBLE);
                mExclusionEditText.setVisibility(View.VISIBLE);
                mExclusionListAddBtn.setVisibility(View.VISIBLE);
                mExclusionListClearBtn.setVisibility(View.VISIBLE);
                mExclusionText.setVisibility(View.VISIBLE);
                mEpcListLayout.setVisibility(View.VISIBLE);
                mExclusionLayout.setVisibility(View.VISIBLE);
                isSelectedEPC =false;
            }
            else {
                mSearchText.setText(getText(R.string.search_description_text));
                mEpcListAddBtn.setVisibility(View.INVISIBLE);
                mEpcListClearBtn.setVisibility(View.INVISIBLE);
                mExclusionEditText.setVisibility(View.INVISIBLE);
                mExclusionListAddBtn.setVisibility(View.INVISIBLE);
                mExclusionListClearBtn.setVisibility(View.INVISIBLE);
                mExclusionText.setVisibility(View.INVISIBLE);
                mEpcListLayout.setVisibility(View.INVISIBLE);
                mExclusionLayout.setVisibility(View.INVISIBLE);
                isSelectedEPC = true;
            }
        }
        else if (buttonView.equals(mSwitchType)) {
            isUF3000 = !isChecked;
        }

        Log.info(END);
    }


    /**
     * デバイス接続 open → claimDevice → setDeviceEnabled → getFirmwareVer →
     * getBatteryLevel
     *
     * @param connectRequestAddress Address
     */
    private void deviceConnect(final String connectRequestAddress) {
        // 結果コード
        int result;
        if (getState() == TecRfidSuite.OPOS_S_CLOSED) {
            if(isUF3000) {
                result = open(DEVICENAME_UF3000, MainActivity.this, mSDKLogLevel, mSDKLogSize);
            }
            else {
                result = open(DEVICENAME, MainActivity.this, mSDKLogLevel, mSDKLogSize);
            }
        } else {
            result = TecRfidSuite.OPOS_SUCCESS;
        }
        // openが成功したら
        if (TecRfidSuite.OPOS_SUCCESS == result) {
            showProgress();
            mClaimDeviceRunnable = new Runnable() {
                public void run() {
                    int claimDeviceResult = claimDevice(connectRequestAddress, mConnectionEventCallback);
                    dismissProgress();
                    // claimDeviceが成功したら
                    if (TecRfidSuite.OPOS_SUCCESS == claimDeviceResult) {
                        mDisconnectFlag = false;
                        // setDeviceEnabledが成功したら
                        if (TecRfidSuite.OPOS_SUCCESS == setDeviceEnabled(true)) {
                            mConnectedString = connectRequestAddress;
                        }
                    } else {
                        close();
                    }
                }
            };
            // claimDeviceを別スレッドで呼ぶ
            mClaimDeviceThread = new Thread(mClaimDeviceRunnable);
            mClaimDeviceThread.start();
        }
    }

    /**
     * デバイス再接続用
     *
     * @param context Context
     */
    public void deviceReConnect(Context context) {
        final String connectRequestAddress = mReConnectString;
        // 結果コード
        int result;
        if (getState() == TecRfidSuite.OPOS_S_CLOSED) {
            result = openReconnect(DEVICENAME_UF3000, MainActivity.this);
        } else {
            result = TecRfidSuite.OPOS_SUCCESS;
        }
        // openが成功したら
        if (TecRfidSuite.OPOS_SUCCESS == result) {
            showProgress(context);
            mClaimDeviceRunnable = new Runnable() {
                public void run() {
                    int claimDeviceResult = claimDeviceReconnect(connectRequestAddress, mConnectionEventCallback);
                    dismissProgress();
                    // claimDeviceが成功したら
                    if (TecRfidSuite.OPOS_SUCCESS == claimDeviceResult) {
                        mDisconnectFlag = false;
                        if (mSettingTool != null) {
                            mSettingTool.reConnectDeviceSuccess();
                        }
                        // setDeviceEnabledが成功したら
                        if (TecRfidSuite.OPOS_SUCCESS == setDeviceEnabledReconnect(true)) {
                            mConnectedString = connectRequestAddress;
                        }
                    } else {
                        if (mSettingTool != null) {
                            mSettingTool.reConnectDeviceFailed();
                        }
                        close();
                    }
                }
            };
            // claimDeviceを別スレッドで呼ぶ
            mClaimDeviceThread = new Thread(mClaimDeviceRunnable);
            mClaimDeviceThread.start();
        }
    }

    /**
     * claimDevice用引数
     *
     * 切断検知コールバック
     * */
    private ConnectionEventHandler mConnectionEventCallback = new ConnectionEventHandler() {
        @Override
        public void onEvent(int state) {
            Log.info(START);
            // オンライン以外なら
            if (state != TecRfidSuite.ConnectStateOnline) {
                mDisconnectFlag = true;
                dismissProgress();
                if (null != mConnectedString) {
                    mReConnectString = mConnectedString;
                }
                mConnectedString = null;
                String message;
                if (state == TecRfidSuite.ConnectStateOffline) {
                    message = getString(R.string.message_connectstate_offline);
                } else {
                    message = getString(R.string.message_connectstate_none);
                }
                if (null != mSettingTool) {
                    mSettingTool.disconnectDevice(getString(R.string.title_error), message,
                            getString(R.string.btn_txt_ok));
                } else {
                    // エラー表示
                    showDialog(getString(R.string.title_error), message, getString(R.string.btn_txt_ok), null);
                }
            } else {
                mDisconnectFlag = false;
            }
            Log.info(END);
        }
    };

    /**
     * デバイス切断
     */
    private void deviceDisConnect() {
        // setDeviceEnabledが行われていれば
        if (isDeviceEnabled()) {
            setDeviceEnabled(false);
        }
        // claimDeviceが行われていれば
        if (isClaimed()) {
            releaseDevice();
        }
        // openが行われていれば
        if (TecRfidSuite.OPOS_S_CLOSED != getState()) {
            close();
        }
        mConnectedString = null;
    }

    /**
     * リスナーをセットする
     *
     * @param listener NotifyForActivityInterface
     */
    public static void setListener(NotifyForActivityInterface listener) {
        mSettingTool = listener;
    }

    /** 切断検知フラグを返す */
    public boolean getDisconnectFlag() {
        return mDisconnectFlag;
    }

    /**
     * 唯一のインスタンスを返す
     *
     * @return mMainActivity インスタンス
     */
    public static MainActivity getInstance() {
        return mMainActivity;
    }

    @Override
    protected void onDestroy() {
        Log.info(START);
        // デバイス切断
//        deviceDisConnect();
        mConnectionEventCallback = null;
        Log.info(END);
        super.onDestroy();

    }

    /** ストレージ書き込みパーミッション用リクエストコード */
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    /**
     * パーミッションをチェックし、許可を促すダイアログを表示する
     */
    private void checkPermission(){
        //権限リクエストが複数ある場合、このリストに追加する
        ArrayList<String> tmpList = new ArrayList<>();
        //権限の有無
        Boolean perExternalStorage = true;
        Boolean perAccessLocation = true;
        Boolean perBluetoothScan = true;
        Boolean perBluetoothConnect = true;
        //前回権限リクエスト拒否の有無
        Boolean rejectExternalStorage = false;
        Boolean rejectAccessLocation = false;
        Boolean rejectBluetoothScan = false;
        Boolean rejectBluetoothConnect = false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            //※ストレージ書き込み権限チェックはAndroid12まで
            //ストレージ書き込み権限チェック
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perExternalStorage = false;
                tmpList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                //前回権限リクエスト拒否しているか
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    rejectExternalStorage = true;
                }
            }
        }
        //位置情報権限チェック
        //※位置情報権限チェックはAndroid11まで
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perAccessLocation = false;
                tmpList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                //前回権限リクエスト拒否しているか
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    rejectAccessLocation = true;
                }
            }
        }
        //Bluetoothの権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //Bluetoothの権限１（BLUETOOTH_SCAN）
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                perBluetoothScan = false;
                tmpList.add(Manifest.permission.BLUETOOTH_SCAN);
                //前回権限リクエスト拒否しているか
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)) {
                    rejectBluetoothScan = true;
                }
            }
            //Bluetoothの権限２（BLUETOOTH_CONNECT）
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                perBluetoothConnect = false;
                tmpList.add(Manifest.permission.BLUETOOTH_CONNECT);
                //前回権限リクエスト拒否しているか
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)) {
                    rejectBluetoothConnect = true;
                }
            }
        }

        //onClick用
        final ArrayList<String> listPermissions = tmpList;

        //ストレージ書き込み、位置情報のどちらかの権限がない、または両方とも権限がない場合、リクエストを行う
        if (!perExternalStorage || !perAccessLocation || !perBluetoothScan || !perBluetoothConnect) {

            //前回権限リクエスト拒否しているか
            if (rejectExternalStorage || rejectAccessLocation || rejectBluetoothScan || rejectBluetoothConnect) {
                requestPermissions(rejectExternalStorage, rejectAccessLocation, (rejectBluetoothScan || rejectBluetoothConnect), listPermissions);
            }
            else {
                //パーミッションのリクエスト
                ActivityCompat.requestPermissions(MainActivity.this, listPermissions.toArray(new String[listPermissions.size()]),
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
        }
    }

    private void requestPermissions(boolean rejectExternalStorage, boolean rejectAccessLocation,boolean rejectBluetooth, final ArrayList<String> listPermissions) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        String strTitle = "";
        String strMsg = "";
        if (rejectExternalStorage) {
            //ストレージ書き込みの拒否
            strTitle = getString(R.string.permission_title);
            strMsg = getString(R.string.permission_message);
        }
        if (rejectAccessLocation) {
            //位置情報の拒否
            if (strTitle.length() > 0) {
                strTitle += "／";
                strMsg += System.getProperty("line.separator") + System.getProperty("line.separator");
            }
            strTitle += getString(R.string.permission_AccessLocation_title);
            strMsg += getString(R.string.permission_AccessLocation_message);
        }
        if (rejectBluetooth) {
            //Bluetoothの権限の拒否
            if (strTitle.length() > 0) {
                strTitle += "／";
                strMsg += System.getProperty("line.separator") + System.getProperty("line.separator");
            }
            strTitle += getString(R.string.permission_Bluetooth_title);
            strMsg += getString(R.string.permission_Bluetooth_message);
        }
        dialog.setTitle(strTitle);
        dialog.setMessage(strMsg);

        dialog.setPositiveButton(R.string.btn_txt_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //パーミッションのリクエスト
                ActivityCompat.requestPermissions(MainActivity.this, listPermissions.toArray(new String[listPermissions.size()]),
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
        });
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        boolean blShowPermissionsResult_Bluetooth = false;

        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION:
                for(int i = 0 ; i < permissions.length ; i++){
                    switch (permissions[i]){
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                //設定ファイル読み込み
                                readInitSettingFile();
                                //許可成功
                                showDialog(null, getString(R.string.permission_success), getString(R.string.btn_txt_ok), null);
                            } else {
                                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                    //許可失敗
                                    showDialog(null, getString(R.string.permission_denied), getString(R.string.btn_txt_ok), null);
                                }
                                else {
                                    //許可失敗
                                    showDialog(null, getString(R.string.permission_failed), getString(R.string.btn_txt_ok), null);
                                }

                            }
                            break;
                        case Manifest.permission.ACCESS_FINE_LOCATION:
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                //許可成功
                                showDialog(null, getString(R.string.permission_AccessLocation_success), getString(R.string.btn_txt_ok), null);
                            } else {
                                if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                                    //許可失敗
                                    showDialog(null, getString(R.string.permission_AccessLocation_denied), getString(R.string.btn_txt_ok), null);
                                }
                                else {
                                    //許可失敗
                                    showDialog(null, getString(R.string.permission_AccessLocation_failed), getString(R.string.btn_txt_ok), null);
                                }

                            }
                            break;
                        case Manifest.permission.BLUETOOTH_SCAN:
                        case Manifest.permission.BLUETOOTH_CONNECT:
                            if (blShowPermissionsResult_Bluetooth == false) {
                                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                    //許可成功
                                    showDialog(null, getString(R.string.permission_Bluetooth_success), getString(R.string.btn_txt_ok), null);
                                } else {
                                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN)) {
                                        //許可失敗
                                        showDialog(null, getString(R.string.permission_Bluetooth_denied), getString(R.string.btn_txt_ok), null);
                                    } else {
                                        //許可失敗
                                        showDialog(null, getString(R.string.permission_Bluetooth_failed), getString(R.string.btn_txt_ok), null);
                                    }
                                }
                                blShowPermissionsResult_Bluetooth = true;       //※権限確認のメッセージが一つしか出ないので、権限リクエストの実行結果も複数出ないようにする
                            }
                            break;

                        default:
                            break;
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * アプリ設定ファイルを読み込む
     *
     * @return 読み込んだ情報
     */
    private void readInitSettingFile() {
        String filePath = mStoragePath + SETTING_PATH + SETTING_FILENAME;
        File file = new File(filePath);
        // ファイルが存在しなければ
        if (!file.exists()) {
            // デフォルト値でファイル作成
            createInitSettingFIle(mStoragePath);
        }
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);
            int index = 0;
            while (br.ready()) {
                String line = br.readLine();
                if (null != line) {
                    if (line.indexOf(KEY_FW_MODE + COMMA) != -1) {
                        index = line.indexOf(KEY_FW_MODE + COMMA);
                        index += (KEY_FW_MODE + COMMA).length();
                        line = line.substring(index);
                        int i = Integer.parseInt(line);
                        SharedPreferences prefs = getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(SEARCH_FW_MODE, i);
                        editor.apply();
                    }else if(line.indexOf(KEY_RADAR_DRAW_MODE + COMMA) != -1) {
                        index = line.indexOf(KEY_RADAR_DRAW_MODE + COMMA);
                        index += (KEY_RADAR_DRAW_MODE + COMMA).length();
                        line = line.substring(index);
                        int i = Integer.parseInt(line);
                        SharedPreferences prefs = getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt(SEARCH_RADAR_DRAW_MODE, i);
                        editor.apply();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != br) {
                    br.close();
                }
                if (null != isr) {
                    isr.close();
                }
                if (null != fis) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * アプリ設定ファイルがなければ、デフォルト値で設定ファイルを作成
     *
     */
    public static void createInitSettingFIle(String storagePath) {
        String filePath = storagePath + SETTING_PATH;
        File dir = new File(filePath);
        boolean isCreate = dir.mkdirs();
        // ディレクトリがないとき
        if (!isCreate && !dir.exists()) {
            // 特にエラーは出さない
            return;
        }

        FileOutputStream fos = null;
        BufferedWriter bw = null;
        OutputStreamWriter osw = null;
        try {
            filePath = filePath + SETTING_FILENAME;
            File file = new File(filePath);
            // ファイルがあれば作りなおし
            if (file.exists()) {
                boolean fileDel = file.delete();
                if (!fileDel) {
                    // 特にエラーは出さない
                    return;
                }
            }
            String writeDate = null;
            writeDate = KEY_FW_MODE + COMMA + DEFAULT_FW_MODE;
            writeDate = writeDate + NEWLINE + KEY_RADAR_DRAW_MODE + COMMA + DEFAULT_RADAR_DRAW_MODE;
            fos = new FileOutputStream(filePath, true);
            osw = new OutputStreamWriter(fos, "UTF-8");
            bw = new BufferedWriter(osw);
            if(writeDate!=null) {
                bw.write(writeDate);
            }
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // 特にエラーは出さない
        } catch (IOException e) {
            e.printStackTrace();
            // 特にエラーは出さない
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != osw) {
                    osw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != bw) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
