package jp.co.toshibatec.uf2200sampleapplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Insets;
import android.graphics.Point;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.DataEventHandler;
import jp.co.toshibatec.callback.ErrorEventHandler;
import jp.co.toshibatec.callback.ResultCallback;
import jp.co.toshibatec.model.TagPack;
import jp.co.toshibatec.uf2200sampleapplication.common.CustomAsyncTask;

public class TagDatabaseActivity extends Activity implements View.OnClickListener {
    FirebaseFirestore firestore;
    private static TagDatabaseActivity mTagDatabaseActivity = null;

    public static TagDatabaseActivity getInstance() {
        return mTagDatabaseActivity;
    }
    /** Save to Database**/
    private ImageView mSaveBtn = null;
    /** ScanEPC**/
    private ImageView mScanBtn = null;
    /** Delete from Database**/
    private ImageView mDeleteBtn = null;

    private EditText mEpcCode = null;

    private EditText mProductCode = null;

    private boolean mIsStartReadTags = false;
//    /** 経過時間表示 */
//    private TextView mElapsedTimeView = null;
    /** 読み取り経過時刻 */
//    private long elapsedTime;
    /** 前回更新時刻 */
    private long prevUpdateTime;
    /** 最大読取数 */
    private final int MAX_READ_COUNT = 99999;
    /** 最大読取時間(ミリ秒) 10000秒 */
    private final long MAX_READ_TIME_MS = 10000000L;

    /** ダイアログ */
    private AlertDialog.Builder mDialog = null;

    /** ダイアログ用ハンドラー */
    private Handler mShowDialogHandler = new Handler(Looper.getMainLooper());

    /** ダイアログ用ランナブル */
    private Runnable mShowDialogRunnable = null;

    /** ライブラリアクセス中プログレス */
    private ProgressBar mProgressBar = null;

    /** プログレス表示フラグ */
    private boolean isShowProgress = false;

    /** プログレスディスミス用ハンドラー */
    private Handler mDissmissProgressHandler = new Handler(Looper.getMainLooper());

    /** プログレスディスミス用ランナブル */
    private Runnable mDissmissProgressRunnable = null;

    /** 読取テスト中にバックキーが押下されたか */
    private boolean isReadBackPress = false;
    /** ボタン切替表示更新用ハンドラー */
    private Handler mButtonViewHandler = new Handler(Looper.getMainLooper());

    /** ボタン切替表示更新用ランナブル */
    private Runnable mButtonViewRunnable = new Runnable() {
        @Override
        public void run() {

            mScanBtn.setBackgroundResource(R.drawable.shape_btn_readtag_readstart);
            mScanBtn.setOnClickListener(TagDatabaseActivity.this);

            mSaveBtn.setBackgroundResource(R.drawable.shape_btn_readtag_save);
            mSaveBtn.setOnClickListener(TagDatabaseActivity.this);

            mDeleteBtn.setBackgroundResource(R.drawable.shape_btn_setting_open);
            mDeleteBtn.setOnClickListener(TagDatabaseActivity.this);
            // 重複排除チェックボックスを有効
            mMenu.clear();
        }
    };

    /** 読取ったタグデータ */
    private ArrayList<String> mReadData = new ArrayList<String>();
    /** 表示しているデータ (重複排除用) */
    private ArrayList<String> mShowReadData = new ArrayList<String>();
    /** アニメーション1 */
    private final int ANIM_1 = 1;
    /** アニメーション2 */
    private final int ANIM_2 = 2;
    /** アニメーション3 */
    private final int ANIM_3 = 3;
    /** アニメーション切替用フラグ */
    private int mAnimFlag = ANIM_1;
    private MenuInflater mInflater;
    private Menu mMenu;
    /** 画像アニメーション用インターバル */
    private final int REPLACEINTERVAL = 500;
    /** 定期処理用ハンドラー */
    private Handler mIntervalHander = new Handler(Looper.getMainLooper());
    /** 定期処理用ランナブル */
    private Runnable mIntervalRunn = new Runnable() {
        @Override
        public void run() {
            if (ANIM_1 == mAnimFlag) {
                mMenu.clear();
                mInflater.inflate(R.menu.conditionmark_2, mMenu);
                mAnimFlag = ANIM_2;
            } else if (ANIM_2 == mAnimFlag) {
                mMenu.clear();
                mInflater.inflate(R.menu.conditionmark_3, mMenu);
                mAnimFlag = ANIM_3;
            } else {
                mMenu.clear();
                mInflater.inflate(R.menu.conditionmark_1, mMenu);
                mAnimFlag = ANIM_1;
            }
            long nowTime = System.currentTimeMillis();
            long diff = nowTime - prevUpdateTime;
            // 経過時間を表示
            int size = mShowReadData.size();
//            if (size < MAX_READ_COUNT) {
//                if ((elapsedTime+diff) < MAX_READ_TIME_MS) {
//                    // 経過時間
//                    elapsedTime += diff;
//                    mElapsedTimeView.setText(String.format("%s", elapsedTime/1000));
//                } else {
//                    // 経過時間
//                    elapsedTime = MAX_READ_TIME_MS;
//                    mElapsedTimeView.setText(String.format("9999"));
//                }
//            }
            prevUpdateTime = nowTime;
            imageReplaceHandler();
        }
    };

    /**
     * startReadTags用引数
     */
    /** 読み取るタグを決定する際にfiltermaskと論理積したものとの比較を行うためのビットパターン */
    private String mFilterID = "00000000";
    /** 読み取るタグを決定する際に論理積を行うためのビットパターン */
    private String mFiltermask = "00000000";
    /** timeout */
    private int mStartReadTagsTimeout = 10000;
    private DataEventHandler mDataEvent = new DataEventHandler() {
        @Override
        public void onEvent(HashMap<String, TagPack> tagList) {
            for (Entry<String, TagPack> e : tagList.entrySet()) {
                /** Get the EPC code from the tag */
                String key = e.getKey();
                Log.d("EPC_SCAN", "EPC Code received: " + key);

                /** Stop reading if an EPC code is scanned */
                if (TecRfidSuite.OPOS_SUCCESS == MainMenuActivity.getSDKLibrary().stopReadTags(mStopReadTagsResultCallback)) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            /** Overwrite the EPC Code Textedit*/
                            mEpcCode = findViewById(R.id.epc_code);
                            mEpcCode.setText(key);
                            showProgress(TagDatabaseActivity.this);
                        }
                    });
                } else {
                    // Run UI updates on the main (UI) thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
                        }
                    });
                }
            }
        }
    };


    /** キャリアセンスエラー */
    private static final int CARRIERSENSEERROR = 19;
    /** 電波出力禁止エラー */
    private static final int WAVEOUTPUTBLOCKERROR = 21;
    /** タグデータバッファフルエラー */
    private static final int TAGDATAFULLBUFFERERROR = 65;
    /** エラーイベント用コールバック(startReadTags用) */
    private ErrorEventHandler mErrorEvent = new ErrorEventHandler() {
        @Override
        public void onEvent(int resultCode, int resultCodeExtended) {
            // startReadTagsが失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != resultCode){
                if (resultCodeExtended != CARRIERSENSEERROR && resultCodeExtended != WAVEOUTPUTBLOCKERROR && resultCodeExtended != TAGDATAFULLBUFFERERROR) {
                    // エラー表示
                    showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_startReadTags), getString(R.string.btn_txt_ok), null);
                }
            }
        }
    };
    /**
     * stopReadTags用引数
     */
    /** 通信実行結果のコールバック */
    private ResultCallback mStopReadTagsResultCallback = new ResultCallback() {
        @Override
        public void onCallback(int resultCode, int resultCodeExtended) {
            // stopReadTagsが失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != resultCode){
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
            }
            // プログレスバーを消去
            dismissProgress();
            mIsStartReadTags = false;
            buttonValid();
            // 読取テスト中にバックキーが押下された場合
            if(isReadBackPress){
                isReadBackPress = false;
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tagdatabase);

        mTagDatabaseActivity = this;

        mSaveBtn = (ImageView) findViewById(R.id.save);
        mSaveBtn.setOnClickListener(this);
        mScanBtn = (ImageView) findViewById(R.id.scan);
        mScanBtn.setOnClickListener(this);
        mDeleteBtn = (ImageView) findViewById(R.id.delete);
        mDeleteBtn.setOnClickListener(this);
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mInflater = getMenuInflater();
        mMenu = menu;
        return super.onCreateOptionsMenu(mMenu);
    }

    public void onClick(View view) {
        if (view.equals(mSaveBtn)) {
            Log.d("clicked", "savebtn");
            firestore = FirebaseFirestore.getInstance();

            mEpcCode = findViewById(R.id.epc_code);
            final String epcCode = mEpcCode.getText().toString();
            mProductCode = findViewById(R.id.product_code);
            final String productCode = mProductCode.getText().toString();

            firestore.collection("tag_list")
                    .whereEqualTo("epc_code", epcCode)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            CollectionReference tag_list = firestore.collection("tag_list");
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    Map<String, Object> tag = new HashMap<>();
                                    tag.put("epc_code", epcCode);
                                    tag.put("product_code", productCode);

                                    firestore.collection("tag_list").add(tag)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    Toast.makeText(getApplicationContext(), "Tag added successfully", Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getApplicationContext(), "Error adding tag", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    // If a tag with the same EPC code already exists, notify the user
                                    Toast.makeText(getApplicationContext(), "Tag with the same EPC code already exists", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                // Handle failure of the query
                                Toast.makeText(getApplicationContext(), "Error checking tag", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        else if (view.equals(mScanBtn)) {
            Log.d("clicked", "scanbtn");
            if (view.equals(mScanBtn)) {
                // startReadTags済みでなければ
                if (!mIsStartReadTags) {
                    mIsStartReadTags = true;
                    prevUpdateTime = System.currentTimeMillis();
                    // 読取停止ボタンへ表示切替
                    mScanBtn.setBackgroundResource(R.drawable.shape_btn_readtag_readstop);
                    mIntervalHander.post(mIntervalRunn);
                    // startReadTagsを失敗した場合
                    if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().startReadTags(mFilterID, mFiltermask, mStartReadTagsTimeout, mDataEvent, mErrorEvent)){
                        // エラー表示
                        showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
                    }
                    // setDataEventEnabledを失敗した場合
                    if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().setDataEventEnabled(true)){
                        // エラー表示
                        showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_setDataEventEnabled), getString(R.string.btn_txt_ok), null);
                    }
                } else {
                    // stopReadTagsを成功した場合
                    if (TecRfidSuite.OPOS_SUCCESS == MainMenuActivity.getSDKLibrary().stopReadTags(mStopReadTagsResultCallback)) {
                        // プログレスバーを表示
                        showProgress(this);
                    } else{
                        // エラー表示
                        showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
                    }
                }
            }
        }
        else if (view.equals(mDeleteBtn)) {
            Log.d("clicked", "deletebtn");
            firestore = FirebaseFirestore.getInstance();

            mEpcCode = findViewById(R.id.epc_code);
            final String epcCode = mEpcCode.getText().toString();

            firestore.collection("tag_list")
                    .whereEqualTo("epc_code", epcCode)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().isEmpty()) {
                                    Toast.makeText(getApplicationContext(), "Tag with the same EPC code do not exists", Toast.LENGTH_LONG).show();
                                } else {
                                    for (DocumentSnapshot document : task.getResult()) {
                                    firestore.collection("tag_list")
                                            .document(document.getId())
                                            .delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getApplicationContext(), "Tag deleted successfully", Toast.LENGTH_LONG).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getApplicationContext(), "Error deleting tag", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            }
                            } else {
                                // Handle failure of the query
                                Toast.makeText(getApplicationContext(), "Error checking tag", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    /**
     * 電波マークアニメーション用
     */
    private void imageReplaceHandler() {
        // 読取中の場合
        if (mIsStartReadTags) {
            // 500ms後mIntervalRunnを実行
            mIntervalHander.postDelayed(mIntervalRunn, REPLACEINTERVAL);
        }
    }

    /**
     * 各ボタンを有効にする
     */
    private void buttonValid() {
        if (null != mButtonViewHandler && null != mButtonViewRunnable) {
            mButtonViewHandler.post(mButtonViewRunnable);
        }
        if (null != mIntervalHander && null != mIntervalRunn) {
            // 電波マークの点灯を停止
            mIntervalHander.removeCallbacks(mIntervalRunn);
        }
        mAnimFlag = ANIM_1;
    }

    /**
     * エラーダイアログ表示
     *
     * @param title 表示タイトル
     * @param message 表示メッセージ
     * @param btn1Txt ボタン1
     * @param btn2Txt ボタン2(不要ならnull)
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt) {
        showDialog(title, message, btn1Txt, btn2Txt, null, null);
    }

    /**
     * エラーダイアログ表示
     *
     * @param title 表示タイトル
     * @param message 表示メッセージ
     * @param btn1Txt ボタン1
     * @param btn2Txt ボタン2(不要ならnull)
     * @param positiveRun OKボタン押下
     * @param negativeRun キャンセルボタン押下
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt, final Runnable positiveRun,  final Runnable negativeRun) {
        if (null != mShowDialogHandler) {
            mShowDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    mDialog = new AlertDialog.Builder(TagDatabaseActivity.this);
                    mDialog.setTitle(title);
                    mDialog.setMessage(message);
                    mDialog.setPositiveButton(btn1Txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (null != positiveRun) {
                                positiveRun.run();
                            }
                        }
                    });
                    if (null != btn2Txt) {
                        mDialog.setNegativeButton(btn2Txt, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (null != negativeRun) {
                                    negativeRun.run();
                                }
                            }
                        });
                    }
                    mDialog.show();
                }
            };
            mShowDialogHandler.post(mShowDialogRunnable);
        }
    }

    /**
     * エラーダイアログ表示
     *
     * @param title 表示タイトル
     * @param message 表示メッセージ
     * @param btn1Txt ボタン1
     * @param btn2Txt ボタン2(不要ならnull)
     * @param isBack 前の画面に戻るか
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt, final boolean isBack) {
        if (null != mShowDialogHandler) {
            mShowDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    mDialog = new AlertDialog.Builder(TagDatabaseActivity.this);
                    mDialog.setTitle(title);
                    mDialog.setMessage(message);
                    mDialog.setPositiveButton(btn1Txt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // なにもしない
                        }
                    });
                    if (null != btn2Txt) {
                        mDialog.setNegativeButton(btn2Txt, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // なにもしない
                            }
                        });
                    }
                    if(isBack) {
                        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        });
                    }
                    mDialog.show();
                }
            };
            mShowDialogHandler.post(mShowDialogRunnable);
        }
    }
    /** アクセス中のプログレス表示 */
    private void showProgress(Context context) {
        if(mProgressBar == null) {
            mProgressBar = new ProgressBar(this,null,android.R.attr.progressBarStyleLarge);
            //スクリーンサイズを取得する
            int width;
            int height;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                WindowMetrics display = this.getWindowManager().getCurrentWindowMetrics();
                // 画面サイズ取得
                Insets insets = display.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
                width = display.getBounds().width() - (insets.right + insets.left);
                height = display.getBounds().height() - (insets.top + insets.bottom);
            } else {
                Display display = this.getWindowManager().getDefaultDisplay();
                Point point = new Point();
                display.getSize(point);
                width = point.x;
                height = point.y;
            }
            //ルートビューにProgressBarを貼り付ける
            ViewGroup rootView = (ViewGroup)getWindow().getDecorView();
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            mProgressBar.setPadding(width*3/8,height*3/8,width*3/8,height*3/8);
            rootView.addView(mProgressBar,params);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        isShowProgress = true;
    }

    /** アクセス中のプログレス消去 */
    private void dismissProgress() {
        mDissmissProgressRunnable = new Runnable() {
            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (null != mProgressBar) {
                    mProgressBar.setVisibility(ProgressBar.GONE);
                }
            }
        };
        if (null != mDissmissProgressHandler) {
            mDissmissProgressHandler.post(mDissmissProgressRunnable);
            // プログレスバー表示フラグをfalse
            isShowProgress = false;
        }
    }
}
