
package jp.co.toshibatec.uf2200sampleapplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.DataEventHandler;
import jp.co.toshibatec.callback.ErrorEventHandler;
import jp.co.toshibatec.callback.ResultCallback;
import jp.co.toshibatec.model.TagPack;
import jp.co.toshibatec.uf2200sampleapplication.common.CustomAsyncTask;
import jp.co.toshibatec.uf2200sampleapplication.common.ExcelReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Insets;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * ---------------------------------------------------------------<br>
 * Copyright (C) 2014<br>
 * TOSHIBA TEC Corporation. All Rights Reserved<br>
 * 1-11-1 Oosaki Shinagawa-ku Tokyo JAPAN<br>
 * ---------------------------------------------------------------<br>
 * SYSTEM NAME :UF-2200<br>
 * SOURCE NAME :InventoryAppActivity.java<br>
 * FIRST AUTHOR :T.Tajima<br>
 * PROGRAMING DATE :2014/06/17<br>
 * DESCRIPTION<br>
 * 読取テスト画面表示クラス<br>
 * CHANGE HISTORY<br>
 * V001.000 2014.06.17 ISB T.Tajima 新規作成<br>
 * V002.000 2021.09.12 ASN T.Kurita 静的解析指摘事項対応<br>
 * ---------------------------------------------------------------<br>
 */
public class InventoryAppActivity extends Activity implements View.OnClickListener, NotifyForActivityInterface {
    FirebaseFirestore firestore;
    /** 読取開始ボタン(読取停止ボタン) */
    private ImageView mReadStartBtn = null;
    /** クリアボタン */
    private ImageView mClearBtn = null;
    /** ソートボタン */
    private ImageView mSortBtn = null;
    /** 保存ボタン */
    private ImageView mSaveBtn = null;
    /** 重複排除チェックボックス */
    private CheckBox mDuplicationCheckbox = null;
    /** 読取中か */
    private boolean mIsStartReadTags = false;
    /** 経過時間表示 */
    private TextView mElapsedTimeView = null;
    /** 読み取り経過時刻 */
    private long elapsedTime;
    /** 前回更新時刻 */
    private long prevUpdateTime;
    /** 最大読取数 */
    private final int MAX_READ_COUNT = 99999;
    /** 最大読取時間(ミリ秒) 10000秒 */
    private final long MAX_READ_TIME_MS = 10000000L;

    /** ストレージパス保存用 */
    private String mStoragePath;

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

    /** 読取保存ディレクトリパス */
    private final String SAVE_READTAG_PATH = "/TEC/tool/readtag/";
    /** 改行 */
    private static final String NEWLINE = "\n";

    /** 読取テスト中にバックキーが押下されたか */
    private boolean isReadBackPress = false;

    private ExcelReader excelReader;
    private Map<String, String> itemCodes = new HashMap<>(); // アイテムコードと品名を格納

    /** 表示更新用ハンドラー */
    private Handler mViewHandler = new Handler(Looper.getMainLooper());
    /** 表示更新用ランナブル */
    private Runnable mViewRunnable = null;
    /** ボタン切替表示更新用ハンドラー */
    private Handler mButtonViewHandler = new Handler(Looper.getMainLooper());
    /** ボタン切替表示更新用ランナブル */
    private Runnable mButtonViewRunnable = new Runnable() {
        @Override
        public void run() {
            // 読取開始ボタンへ表示切替
            mReadStartBtn.setBackgroundResource(R.drawable.shape_btn_readtag_readstart);
            mReadStartBtn.setOnClickListener(InventoryAppActivity.this);
            // クリアボタンをアクティブ
            mClearBtn.setBackgroundResource(R.drawable.shape_btn_readtag_clear);
            mClearBtn.setOnClickListener(InventoryAppActivity.this);
            // ソートボタンをアクティブ
            mSortBtn.setBackgroundResource(R.drawable.shape_btn_readtag_sort);
            mSortBtn.setOnClickListener(InventoryAppActivity.this);
            // 保存ボタンをアクティブ
            mSaveBtn.setBackgroundResource(R.drawable.shape_btn_readtag_save);
            mSaveBtn.setOnClickListener(InventoryAppActivity.this);
            // 重複排除チェックボックスを有効
            mDuplicationCheckbox.setEnabled(true);
            mMenu.clear();
        }
    };

    /** 読取ったタグデータ */
    private ArrayList<String> mReadData = new ArrayList<String>();
    /** 表示しているデータ (重複排除用) */
    private ArrayList<String> mShowReadData = new ArrayList<String>();
    /** 読取タグデータアダプター　 */
    private ShowReadAdapter mShowReadAdapter = null;
    /** タグリスト表示ビュー */
    private ListView mListView = null;
    /** 読取タグリストを表示更新用タスク */
    private UpdateReadTagDataTask mUpdateReadTagDataTask = null;
    /** 読取タグリストを表示更新用タスク Android11対応 */
    private CustomUpdateReadTagDataTask mCustomUpdateReadTagDataTask = null;
    /** 読取数 */
    private TextView mReadCount = null;
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
            if (size < MAX_READ_COUNT) {
                if ((elapsedTime+diff) < MAX_READ_TIME_MS) {
                    // 経過時間
                    elapsedTime += diff;
                    mElapsedTimeView.setText(String.format("%s", elapsedTime/1000));
                } else {
                    // 経過時間
                    elapsedTime = MAX_READ_TIME_MS;
                    mElapsedTimeView.setText(String.format("9999"));
                }
            }
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
    /** データイベント用コールバック */
    private DataEventHandler mDataEvent = new DataEventHandler() {
        @Override
        public void onEvent(HashMap<String, TagPack> tagList) {
            for (Entry<String, TagPack> e : tagList.entrySet()) {
                // 受信データからタグ情報を取得
                String key = e.getKey();
                // add conditional statement here
                // 追加
                mReadData.add(key);
            }
            // 読取タグリストを表示更新
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                mCustomUpdateReadTagDataTask = new CustomUpdateReadTagDataTask();
                mCustomUpdateReadTagDataTask.execute("");
            } else {
                mUpdateReadTagDataTask = new UpdateReadTagDataTask();
                mUpdateReadTagDataTask.execute("");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventoryapp);
        firestore = FirebaseFirestore.getInstance();
        excelReader = new ExcelReader(this);
        Map<String, List<List<String>>> data = excelReader.readExcelFile("RFID_Item_data.xlsx");

        // Sheet2からアイテムコードと品名を取得
        List<List<String>> sheetData = data.get("Sheet2");
        if (sheetData != null) {
            for (List<String> row : sheetData) {
                if (row.size() >= 2) {
                    String itemCode = row.get(0);
                    String itemName = row.get(1);
                    itemCodes.put(itemCode, itemName); // アイテムコードをマップに追加
                }
            }
        }
        Log.d("excel", itemCodes.toString());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            mStoragePath = getApplicationContext().getExternalFilesDir(null) + "";
        } else {
            mStoragePath = Environment.getExternalStorageDirectory() + "";
        }

        mReadStartBtn = (ImageView) findViewById(R.id.readstart);
        mReadStartBtn.setOnClickListener(this);
        Object omListView = findViewById(R.id.lv_readtag);
        mElapsedTimeView = (TextView) findViewById(R.id.elapsedTime);
        if (omListView instanceof ListView) {
            mListView = (ListView) omListView;
        } else {
            mListView = new ListView(InventoryAppActivity.this);
        }
        Object omClearBtn = findViewById(R.id.clear);
        if (omClearBtn instanceof ImageView) {
            mClearBtn = (ImageView) omClearBtn;
        } else {
            mClearBtn = new ImageView(InventoryAppActivity.this);
        }
        mClearBtn.setOnClickListener(null);
        Object omSortBtn = findViewById(R.id.sort);
        if (omSortBtn instanceof ImageView) {
            mSortBtn = (ImageView) omSortBtn;
        } else {
            mSortBtn = new ImageView(InventoryAppActivity.this);
        }
        mSortBtn.setOnClickListener(null);
        Object omSaveBtn = findViewById(R.id.save);
        if (omSaveBtn instanceof ImageView) {
            mSaveBtn = (ImageView) omSaveBtn;
        } else {
            mSaveBtn = new ImageView(InventoryAppActivity.this);
        }
        mSaveBtn.setOnClickListener(null);
        Object omDuplicationCheckbox = findViewById(R.id.duplication_checkbox);
        if (omDuplicationCheckbox instanceof CheckBox) {
            mDuplicationCheckbox = (CheckBox) omDuplicationCheckbox;
        } else {
            mDuplicationCheckbox = new CheckBox(InventoryAppActivity.this);
        }
        mDuplicationCheckbox.setOnClickListener(this);
        Object omReadCount = findViewById(R.id.readcount);
        if (omReadCount instanceof TextView) {
            mReadCount = (TextView) omReadCount;
        } else {
            mReadCount = new TextView(InventoryAppActivity.this);
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mShowReadAdapter = new ShowReadAdapter(this, mShowReadData);
        mListView.setAdapter(mShowReadAdapter);

        // リスナーを登録
        MainMenuActivity.setListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mInflater = getMenuInflater();
        mMenu = menu;
        return super.onCreateOptionsMenu(mMenu);
    }

    @Override
    public void onClick(View view) {
        // 読取開始(読取停止)ボタンが押下された場合
        if (view.equals(mReadStartBtn)) {
            // startReadTags済みでなければ
            if (!mIsStartReadTags) {
                mIsStartReadTags = true;
                prevUpdateTime = System.currentTimeMillis();
                // 読取停止ボタンへ表示切替
                mReadStartBtn.setBackgroundResource(R.drawable.shape_btn_readtag_readstop);
                // クリアボタンをグレーアウト
                mClearBtn.setBackgroundResource(R.drawable.and_btn_clear_g);
                mClearBtn.setOnClickListener(null);
                // ソートボタンをグレーアウト
                mSortBtn.setBackgroundResource(R.drawable.and_btn_sort_g);
                mSortBtn.setOnClickListener(null);
                // 保存ボタンをグレーアウト
                mSaveBtn.setBackgroundResource(R.drawable.and_btn_save2_g);
                mSaveBtn.setOnClickListener(null);
                // 重複排除チェックボックスを無効
                mDuplicationCheckbox.setEnabled(false);
                // 電波マークを点灯
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
        else if (view.equals(mClearBtn)) {
            clear();
        } else if (view.equals(mSortBtn)) {
            updateReadTagDataSort();
        } else if (view.equals(mSaveBtn)) {
            // 表示されているタグリストをテキスト形式で保存
            saveSettingText();
        }
    }

    /**
     * クリア処理
     */
    private void clear() {
        // 読取ったタグデータを破棄
        mReadData.clear();
        // 表示しているデータを破棄
        mShowReadData.clear();
        mShowReadAdapter.clear();
        // 読取数をクリア
        mReadCount.setText("" + mShowReadAdapter.getCount());
        // クリアボタンをグレーアウト
        mClearBtn.setBackgroundResource(R.drawable.and_btn_clear_g);
        mClearBtn.setOnClickListener(null);
        // ソートボタンをグレーアウト
        mSortBtn.setBackgroundResource(R.drawable.and_btn_sort_g);
        mSortBtn.setOnClickListener(null);
        // 保存ボタンをグレーアウト
        mSaveBtn.setBackgroundResource(R.drawable.and_btn_save2_g);
        mSaveBtn.setOnClickListener(null);
        // 時間を0sにリセット
        elapsedTime = 0;
        mElapsedTimeView.setText(String.format("%s", elapsedTime/1000));
    }

    /**
     * ソートした読取タグリストを表示更新
     */
    private void updateReadTagDataSort() {
        if (null != mViewHandler) {
            mViewRunnable = new Runnable() {
                @Override
                public void run() {
                    // 読取ったタグデータをソート
                    Collections.sort(mShowReadData);
                    mShowReadAdapter.notifyDataSetChanged();
                }
            };
            mViewHandler.post(mViewRunnable);
        }
    }

    /**
     * テキスト形式の設定ファイルをSDへ保存
     */
    private void saveSettingText() {
        if (0 == mShowReadData.size()) {
            showDialog(getString(R.string.title_error), getString(R.string.message_notag_error), getString(R.string.btn_txt_ok), null);
            return;
        }

        // ファイル名入力用エディットテキスト
        final EditText editView = new EditText(InventoryAppActivity.this);
        new AlertDialog.Builder(InventoryAppActivity.this).setTitle(getString(R.string.title_savedialog))
                // setViewにてビューを設定します。
                .setView(editView).setPositiveButton(getString(R.string.btn_txt_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String filePath = mStoragePath + SAVE_READTAG_PATH;
                        File file = new File(filePath);
                        boolean isCreate = file.mkdirs();
                        // ディレクトリがないとき
                        if (!isCreate && !file.exists()) {
                            // エラー表示
                            showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null);
                            return;
                        }

                        final String newFilePath = filePath + editView.getText().toString() + ".txt";
                        final File newFile = new File(newFilePath);
                        // 同名ファイルがある場合
                        if (newFile.exists()) {
                            // 上書きするか表示
                            showDialog(getString(R.string.title_savesetting), getString(R.string.message_savesetting_override), getString(R.string.btn_txt_ok), getString(R.string.btn_txt_cancel), new Runnable() {
                                @Override
                                public void run() {
                                    // OKボタン押下
                                    // ファイルを削除
                                    boolean fileDir = newFile.delete();
                                    if (fileDir) {
                                        createNewFile(newFilePath);
                                    } else {
                                        // エラー表示
                                        showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null);
                                    }
                                }
                            }, new Runnable() {
                                @Override
                                public void run() {
                                    // キャンセルボタン押下
                                    return;
                                }
                            });
                        } else {
                            createNewFile(newFilePath);
                        }
                    }
                }).setNegativeButton(getString(R.string.btn_txt_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // なにもしない
                    }
                }).show();
    }

    /**
     * 新しいファイルを作成
     *
     * @param filePath 新規ファイルパス
     */
    private void createNewFile(String filePath) {
        FileOutputStream fos = null;
        BufferedWriter bw = null;
        Exception error = null;
        OutputStreamWriter osw = null;
        try {
            // 作成したファイルが PC で見えるように認識させる。
            MediaScannerConnection.scanFile(this, new String[] { filePath }, null, null);

            fos = new FileOutputStream(filePath, true);
            osw = new OutputStreamWriter(fos, "UTF-8");
            bw = new BufferedWriter(osw);
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < mShowReadData.size(); ++i) {
                buf.append(mShowReadData.get(i) + NEWLINE);
            }
            String str = buf.toString();
            bw.write(str);
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            error = e;
        } catch (IOException e) {
            e.printStackTrace();
            error = e;
        } finally {
            try {
                if (null != bw) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        }
        if(error != null) {
            // エラー表示
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null);
        }
        else {
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_savefile), getString(R.string.btn_txt_ok), null);
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

    @Override
    public void onBackPressed() {
        if (isShowProgress) {
            //ProgressBarを消す
            dismissProgress();
        }
        // 読取中の場合
        if (mIsStartReadTags) {
            mIsStartReadTags = false;
            isReadBackPress = true;
            // stopReadTagsを失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().stopReadTags(mStopReadTagsResultCallback)){
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
            } else{
                showProgress(InventoryAppActivity.this);
            }
        } else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        mShowDialogHandler.removeCallbacks(mShowDialogRunnable);
        mShowDialogHandler = null;
        mShowDialogRunnable = null;
        mIntervalHander.removeCallbacks(mIntervalRunn);
        mIntervalHander = null;
        mIntervalRunn = null;
        mViewHandler.removeCallbacks(mViewRunnable);
        mViewHandler = null;
        mViewRunnable = null;
        mButtonViewHandler.removeCallbacks(mButtonViewRunnable);
        mButtonViewHandler = null;
        mButtonViewRunnable = null;
        mDissmissProgressHandler.removeCallbacks(mDissmissProgressRunnable);
        mDissmissProgressHandler = null;
        mDissmissProgressRunnable = null;
        mDataEvent = null;
        mErrorEvent = null;
        // 読取中の場合
        if (mIsStartReadTags) {
            mIsStartReadTags = false;
            // stopReadTagsを失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().stopReadTags(mStopReadTagsResultCallback)){
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_stopReadTags), getString(R.string.btn_txt_ok), null);
            }
        }
        mStopReadTagsResultCallback = null;
        MainMenuActivity.setListener(null);
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // 前画面に戻る
        onBackPressed();
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void disconnectDevice(String title, String message, String btn1) {
        // エラー表示
        showDialog(title, message, btn1, null,true);
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
                    mDialog = new AlertDialog.Builder(InventoryAppActivity.this);
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
                    mDialog = new AlertDialog.Builder(InventoryAppActivity.this);
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

    /**
     * 読取タグ表示時のビープ音を鳴らす
     */
    private void soundBeep() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        toneGenerator.release();
    }

    /**
     * 読取タグデータアダプタークラス
     */
    private class ShowReadAdapter extends ArrayAdapter<String> {

        /** 表示タグデータ */
        private ArrayList<String> mItem = null;

        public ShowReadAdapter(Context context, ArrayList<String> objects) {
            super(context, 0, objects);

            mItem = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TableRow tr = null;
            // ビューが再利用できれば再利用する
            if (null != convertView && convertView instanceof TableRow) {
                tr = (TableRow) convertView;
            } else {
                Object oTr = getLayoutInflater().inflate(R.layout.readtaglist, null);
                if (oTr instanceof TableRow) {
                    tr = (TableRow) oTr;
                } else {
                    tr = new TableRow(InventoryAppActivity.this);
                }
                tr.setBackgroundResource(R.drawable.underline);
            }
            TextView tvDevaiceName = null;
            Object oTvDevaiceName = tr.findViewById(R.id.readtagname);
            if (oTvDevaiceName instanceof TextView) {
                tvDevaiceName = (TextView) oTvDevaiceName;
            } else {
                tvDevaiceName = new TextView(InventoryAppActivity.this);
            }
            // テキストにタグ情報をセット
            tvDevaiceName.setText(mItem.get(position));

            return tr;
        }
    }

    /**
     * 読取タグリストを表示更新
     */
    private class UpdateReadTagDataTask extends AsyncTask<String, String, Long> {

        @Override
        protected void onPostExecute(Long result) {
            // setDataEventEnabledを失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().setDataEventEnabled(true)){
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_setDataEventEnabled), getString(R.string.btn_txt_ok), null);
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // 追加・更新しない
            int size = mShowReadData.size();
            if (elapsedTime >= MAX_READ_TIME_MS || size >= MAX_READ_COUNT) {
                return;
            }
            // リストビューに追加する
            for (int i = 0; i < values.length; i++) {
                if(null != values[i] && 0 != values[i].length()){
                    if (mDuplicationCheckbox.isChecked()) {
                        if (mShowReadData.indexOf(values[i]) == -1) {
                            size = mShowReadData.size();
                            if (size >= MAX_READ_COUNT){
                                // タグ表示最大値に達していたら追加しない
                                break;
                            }
                            // アダプターへデータを追加
                            mShowReadData.add(values[i]);
                        }
                    }
                    else {
                        size = mShowReadData.size();
                        if (size >= MAX_READ_COUNT){
                            // タグ表示最大値に達していたら追加しない
                            break;
                        }
                        // アダプターへデータを追加
                        mShowReadData.add(values[i]);
                    }
                }
            }
            // アダプターを更新
            mShowReadAdapter.notifyDataSetChanged();
            // 読取数を更新
            size = mShowReadAdapter.getCount();
            mReadCount.setText("" + size);
            super.onProgressUpdate(values);
        }

        @Override
        protected Long doInBackground(String... params) {

            ArrayList<String> a = new ArrayList<String>();
            // 重複排除にチェックがはいっていれば
            if (mDuplicationCheckbox.isChecked()) {
                // 新しい読取タグデータ分ループ
                for (int i = 0; i < mReadData.size(); i++) {
                    // 重複していなければ
                    if (-1 == mShowReadData.indexOf(mReadData.get(i))) {
                        a.add(mReadData.get(i));
                        // 50個追加された場合
                        if (a.size() >= 50) {
                            // 表示更新
                            publishProgress(a.toArray(new String[a.size()]));
                            a.clear();
                            // ビープ音鳴音
                            soundBeep();
                        }
                    }
                }
            }
            else {
                // 新しい読取タグデータ分ループ
                for (int i = 0; i < mReadData.size(); i++) {
                    a.add(mReadData.get(i));
                    // 50個追加された場合
                    if (a.size() >= 50) {
                        // 表示更新
                        publishProgress(a.toArray(new String[a.size()]));
                        a.clear();
                        // ビープ音鳴音
                        soundBeep();
                    }
                }
            }
            // 50個未満で表示更新が済んでないタグ情報がある場合
            if(!a.isEmpty()){
                // 表示更新
                publishProgress(a.toArray(new String[a.size()]));
                a.clear();
                // ビープ音鳴音
                soundBeep();
            }

            // 読取分のが表示更新が済んだので、クリア
            mReadData.clear();
            return null;
        }
    }
    /**
     * 読取タグリストを表示更新
     */
    private class CustomUpdateReadTagDataTask extends CustomAsyncTask<String, String, Long> {

        @Override
        protected void onPostExecute(Long result) {
            // setDataEventEnabledを失敗した場合
            if (TecRfidSuite.OPOS_SUCCESS != MainMenuActivity.getSDKLibrary().setDataEventEnabled(true)){
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_processfailed_setDataEventEnabled), getString(R.string.btn_txt_ok), null);
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            // 追加・更新しない
            int size = mShowReadData.size();
            if (elapsedTime >= MAX_READ_TIME_MS || size >= MAX_READ_COUNT) {
                return;
            }
            // リストビューに追加する
            for (int i = 0; i < values.length; i++) {
                if(null != values[i] && 0 != values[i].length()){
                    if (mDuplicationCheckbox.isChecked()) {
                        if (mShowReadData.indexOf(values[i]) == -1) {
                            size = mShowReadData.size();
                            if (size >= MAX_READ_COUNT){
                                // タグ表示最大値に達していたら追加しない
                                break;
                            }
                            // アダプターへデータを追加
                            mShowReadData.add(values[i]);
                        }
                    }
                    else {
                        size = mShowReadData.size();
                        if (size >= MAX_READ_COUNT){
                            // タグ表示最大値に達していたら追加しない
                            break;
                        }
                        // アダプターへデータを追加
                        mShowReadData.add(values[i]);
                    }
                }
            }
            // アダプターを更新
            mShowReadAdapter.notifyDataSetChanged();
            // 読取数を更新
            size = mShowReadAdapter.getCount();
            mReadCount.setText("" + size);
            super.onProgressUpdate(values);
        }

        @Override
        protected Long doInBackground(String... params) {

            ArrayList<String> a = new ArrayList<String>();
            // 重複排除にチェックがはいっていれば
            if (mDuplicationCheckbox.isChecked()) {
                // 新しい読取タグデータ分ループ
                for (int i = 0; i < mReadData.size(); i++) {
                    // 重複していなければ
                    if (-1 == mShowReadData.indexOf(mReadData.get(i))) {
                        a.add(mReadData.get(i));
                        // 50個追加された場合
                        if (a.size() >= 50) {
                            // 表示更新
                            publishProgress(a.toArray(new String[a.size()]));
                            a.clear();
                            // ビープ音鳴音
                            soundBeep();
                        }
                    }
                }
            }
            else {
                // 新しい読取タグデータ分ループ
                for (int i = 0; i < mReadData.size(); i++) {
                    a.add(mReadData.get(i));
                    // 50個追加された場合
                    if (a.size() >= 50) {
                        // 表示更新
                        publishProgress(a.toArray(new String[a.size()]));
                        a.clear();
                        // ビープ音鳴音
                        soundBeep();
                    }
                }
            }
            // 50個未満で表示更新が済んでないタグ情報がある場合
            if(!a.isEmpty()){
                // 表示更新
                publishProgress(a.toArray(new String[a.size()]));
                a.clear();
                // ビープ音鳴音
                soundBeep();
            }

            // 読取分のが表示更新が済んだので、クリア
            mReadData.clear();
            return null;
        }
    }
}
