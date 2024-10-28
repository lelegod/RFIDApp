
package jp.co.toshibatec.uf2200sampleapplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.callback.ReadBarcodeCallback;
import jp.co.toshibatec.uf2200sampleapplication.common.CustomAsyncTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

public class ReadBarcodeActivity extends Activity implements View.OnClickListener, NotifyForActivityInterface {

    private Menu mMenu;
    private MenuInflater mInflater;

    /** 表示しているデータ */
    private ArrayList<BarcodeInfo> mShowReadData = new ArrayList<BarcodeInfo>();
    /** バーコードデータアダプター */
    private ShowReadAdapter mShowReadAdapter = null;

    /** バーコードリスト表示ビュー */
    private ListView mListView = null;
    /** ダイアログ */
    private AlertDialog.Builder mDialog = null;

    /** ダイアログ用ハンドラー */
    private Handler mShowDialogHandler = new Handler(Looper.getMainLooper());
    /** ダイアログ用ランナブル */
    private Runnable mShowDialogRunnable = null;

    /** プログレスディスミス用ハンドラー */
    private Handler mDissmissProgressHandler = new Handler(Looper.getMainLooper());
    /** プログレスディスミス用ランナブル */
    private Runnable mDissmissProgressRunnable = null;
    /** 読取開始ボタン(読取停止ボタン) */
    private ImageView mReadStartBtn = null;
    /** クリアボタン */
    private ImageView mClearBtn = null;
    /** ソートボタン */
    private ImageView mSortBtn = null;
    /** 保存ボタン */
    private ImageView mSaveBtn = null;

    /** ストレージパス保存用 */
    private String mStoragePath;

    /** ボタン切替表示有効更新用ハンドラー */
    private Handler mButtonValidViewHandler = new Handler(Looper.getMainLooper());
    /** ボタン切替表示有効更新用ランナブル */
    private Runnable mButtonValidViewRunnable = new Runnable() {
        @Override
        public void run() {
            // 読取開始ボタンへ表示切替
            mReadStartBtn.setBackgroundResource(R.drawable.shape_btn_readtag_readstart);
            mReadStartBtn.setOnClickListener(ReadBarcodeActivity.this);
            if(mShowReadAdapter.getCount()>0) {
                // クリアボタンをアクティブ
                mClearBtn.setBackgroundResource(R.drawable.shape_btn_readtag_clear);
                mClearBtn.setOnClickListener(ReadBarcodeActivity.this);
                // ソートボタンをアクティブ
                mSortBtn.setBackgroundResource(R.drawable.shape_btn_readtag_sort);
                mSortBtn.setOnClickListener(ReadBarcodeActivity.this);
                // 保存ボタンをアクティブ
                mSaveBtn.setBackgroundResource(R.drawable.shape_btn_readtag_save);
                mSaveBtn.setOnClickListener(ReadBarcodeActivity.this);
            }
        }
    };

    /** 読取タグリストを表示更新用タスク */
    private UpdateReadBarcodeDataTask mUpdateReadBarcodeDataTask = null;
    /** 読取タグリストを表示更新用タスク APIレベル30以上用 */
    private CustomUpdateReadBarcodeDataTask mCustomUpdateReadBarcodeDataTask = null;

    /** 読取中か */
    private boolean mIsStartReadBarcode = false;

    /** 読取数 */
    private TextView mReadCount = null;

    /** 表示更新用ハンドラー */
    private Handler mViewHandler = new Handler(Looper.getMainLooper());
    /** 表示更新用ランナブル */
    private Runnable mViewRunnable = null;
    /** ファイル名テンプレート */
    private final String TEMP_FILENAME = "ReadBarcode";
    /** 読取保存ディレクトリパス */
    private final String SAVE_READBARCODE_PATH = "/TEC/tool/readbarcode/";
    /** テキスト書込み用 */
    private final String NEWLINE = "\n";
    /** テキスト書込み用 */
    private final String COMMA = ",";
    /** バーコード表示最大値 */
    private int mTableLayoutMaxBarcode =DEFAULT_TABLELAYOUTMAX_BARCODE;
    /** バーコード表示最大値(デフォルト) */
    public static final int DEFAULT_TABLELAYOUTMAX_BARCODE = 10000;
    /** テキスト書込み用 */
    public final static String KEY_LISTMAX_BARCODE = "ListMaxBarcode";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode);
        //メインメニューに戻る
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            mStoragePath = getApplicationContext().getExternalFilesDir(null) + "";
        } else {
            mStoragePath = Environment.getExternalStorageDirectory() + "";
        }

        Object omReadStartBtn = findViewById(R.id.readstart);
        if (omReadStartBtn instanceof ImageView) {
            mReadStartBtn = (ImageView) omReadStartBtn;
        } else {
            mReadStartBtn = new ImageView(ReadBarcodeActivity.this);
        }
        mReadStartBtn.setOnClickListener(this);
        Object omClearBtn = findViewById(R.id.clear);
        if (omClearBtn instanceof ImageView) {
            mClearBtn = (ImageView) omClearBtn;
        } else {
            mClearBtn = new ImageView(ReadBarcodeActivity.this);
        }
        mClearBtn.setOnClickListener(null);
        Object omSortBtn = findViewById(R.id.sort);
        if (omSortBtn instanceof ImageView) {
            mSortBtn = (ImageView) omSortBtn;
        } else {
            mSortBtn = new ImageView(ReadBarcodeActivity.this);
        }
        mSortBtn.setOnClickListener(null);
        Object omSaveBtn = findViewById(R.id.save);
        if (omSaveBtn instanceof ImageView) {
            mSaveBtn = (ImageView) omSaveBtn;
        } else {
            mSaveBtn = new ImageView(ReadBarcodeActivity.this);
        }

        Object omReadCount = findViewById(R.id.readcount);
        if (omReadCount instanceof TextView) {
            mReadCount = (TextView) omReadCount;
        } else {
            mReadCount = new TextView(ReadBarcodeActivity.this);
        }

        Object omListView = findViewById(R.id.lv_readtag);
        if (omListView instanceof ListView) {
            mListView = (ListView) omListView;
        } else {
            mListView = new ListView(ReadBarcodeActivity.this);
        }

        mShowReadAdapter = new ShowReadAdapter(this, mShowReadData);
        mListView.setAdapter(mShowReadAdapter);

        mTableLayoutMaxBarcode = getIntent().getIntExtra(KEY_LISTMAX_BARCODE, DEFAULT_TABLELAYOUTMAX_BARCODE);

        // リスナーを登録
        MainMenuActivity.setListener(this);
    }

    @Override
    protected void onDestroy() {
        mShowDialogHandler.removeCallbacks(mShowDialogRunnable);
        mShowDialogHandler = null;
        mShowDialogRunnable = null;
        mDissmissProgressHandler.removeCallbacks(mDissmissProgressRunnable);
        mDissmissProgressHandler = null;
        mDissmissProgressRunnable = null;
        mViewHandler.removeCallbacks(mViewRunnable);
        mViewHandler = null;
        mViewRunnable = null;
        mButtonValidViewHandler.removeCallbacks(mButtonValidViewRunnable);
        mButtonValidViewHandler = null;
        mButtonValidViewRunnable = null;

        if (mIsStartReadBarcode) {
            MainMenuActivity.getSDKLibrary().stopReadBarcode(mStopReadBarcodeCallBack);
        }

        readBarcodeCallBack = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (mCustomUpdateReadBarcodeDataTask != null) {
                mCustomUpdateReadBarcodeDataTask.cancel(true);
                mCustomUpdateReadBarcodeDataTask = null;
            }
        } else{
            if(mUpdateReadBarcodeDataTask != null){
                if (mUpdateReadBarcodeDataTask.getStatus() == AsyncTask.Status.RUNNING) {
                    mUpdateReadBarcodeDataTask.cancel(true);
                }
            	mUpdateReadBarcodeDataTask = null;
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //バーコードマーク表示
        mMenu = menu;
        mInflater = getMenuInflater();
        mInflater.inflate(R.menu.barcodemark, mMenu);

        return super.onCreateOptionsMenu(mMenu);
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mReadStartBtn)) {
            if(mIsStartReadBarcode == false) {
                //読み込み開始
                mIsStartReadBarcode = true;
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

                // バーコード読み込み開始
                if (MainMenuActivity.getSDKLibrary().startReadBarcode(readBarcodeCallBack,ReadBarcodeActivity.this) !=TecRfidSuite.OPOS_SUCCESS) {
                    mIsStartReadBarcode = false;
                    if(mShowReadAdapter.getCount()>0) {
                        buttonValid();
                    }
                    showDialog(getString(R.string.title_error), getString(R.string.message_barcode_startRead_error), getString(R.string.btn_txt_ok), null,false);
                    return;
                }
            } else {
                //読み込み停止
                if (MainMenuActivity.getSDKLibrary().stopReadBarcode(mStopReadBarcodeCallBack) != TecRfidSuite.OPOS_SUCCESS) {
                    showDialog(getString(R.string.title_error), getString(R.string.message_barcode_stopRead_error), getString(R.string.btn_txt_ok), null,false);
                }
            }
        } else if (view.equals(mClearBtn)) {
            // 表示しているデータを破棄
            mShowReadData.clear();
            mShowReadAdapter.clear();
            // クリアボタンをグレーアウト
            mClearBtn.setBackgroundResource(R.drawable.and_btn_clear_g);
            mClearBtn.setOnClickListener(null);
            // ソートボタンをグレーアウト
            mSortBtn.setBackgroundResource(R.drawable.and_btn_sort_g);
            mSortBtn.setOnClickListener(null);
            // 保存ボタンをグレーアウト
            mSaveBtn.setBackgroundResource(R.drawable.and_btn_save2_g);
            mSaveBtn.setOnClickListener(null);
            // 読取数をクリア
            mReadCount.setText("" + mShowReadAdapter.getCount());
        } else if (view.equals(mSortBtn)) {
            updateReadTagDataSort();
        } else if (view.equals(mSaveBtn)) {
            // 表示されているタグリストをテキスト形式で保存
            saveSettingText();
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
        showDialog(title, message, btn1Txt, btn2Txt, null, null,isBack);
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
     * @param isBack 前の画面に戻るか
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt, final Runnable positiveRun,  final Runnable negativeRun, final boolean isBack) {
        if (null != mShowDialogHandler) {
            mShowDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    mDialog = new AlertDialog.Builder(ReadBarcodeActivity.this);
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

    private ReadBarcodeCallback readBarcodeCallBack = new ReadBarcodeCallback(){
        @Override
        public void onCallback(HashMap<String, String> barcodedata, int resultCode, int resultCodeExtended) {
            if(resultCode==0) {
                if(barcodedata != null){
                    BarcodeInfo barcodeinfo = new BarcodeInfo(barcodedata.get(TecRfidSuite.BarcodePackKeyType)
                            ,barcodedata.get(TecRfidSuite.BarcodePackKeyLength)
                            ,barcodedata.get(TecRfidSuite.BarcodePackKeyBarcode));
                    mShowReadData.add(barcodeinfo);
                    // タグ表示最大値に達していたら
                    if (mShowReadAdapter.getCount() > mTableLayoutMaxBarcode) {
                        // 先頭のタグ情報を削除
                        mShowReadData.remove(0);
                    }
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                        mCustomUpdateReadBarcodeDataTask = new CustomUpdateReadBarcodeDataTask();
                        mCustomUpdateReadBarcodeDataTask.execute("");
                    } else {
                        mUpdateReadBarcodeDataTask = new UpdateReadBarcodeDataTask();
                        mUpdateReadBarcodeDataTask.execute("");
                    }
                }
            }
        }
    };

    /** バーコード継続読取停止用コールバック*/
    private ReadBarcodeCallback mStopReadBarcodeCallBack = new ReadBarcodeCallback(){
        @Override
        public void onCallback(HashMap<String, String> barcodedata, int resultCode, int resultCodeExtended) {
            if(resultCode == TecRfidSuite.OPOS_SUCCESS) {
                mIsStartReadBarcode = false;
                buttonValid();
            }
            else {
                // エラー表示
                showDialog(getString(R.string.title_error), getString(R.string.message_barcode_stopRead_error),
                        getString(R.string.btn_txt_ok), null,false);

            }
        }
    };

    @Override
    public void disconnectDevice(String title, String message, String btn1) {
        mIsStartReadBarcode =false;
        showDialog(title, message, btn1, null,true);
    }

    @Override
    public void onBackPressed() {
        if(mIsStartReadBarcode) {
            // エラー表示
            showDialog(getString(R.string.title_error), getString(R.string.message_barcode_reading_error), getString(R.string.btn_txt_ok), null,false);
        }
        else  {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        onBackPressed();
        return super.onMenuItemSelected(featureId, item);
    }

    private void buttonValid() {
        if (null != mButtonValidViewHandler && null != mButtonValidViewRunnable) {
            mButtonValidViewHandler.post(mButtonValidViewRunnable);
        }
    }

    /**
     * ソートした読取タグリストを表示更新
     */
    private void updateReadTagDataSort() {
        if (null != mViewHandler) {
            mViewRunnable = new Runnable() {
                @Override
                public void run() {
                    // 表示データを一時領域にコピー
                    ArrayList<BarcodeInfo> list = new ArrayList<BarcodeInfo>();
                    for(int i = 0 ; i < mShowReadData.size() ; i++){
                        list.add(mShowReadData.get(i));
                    }

                    // 一時領域データをソート
                    list = sortBarcodeInfo(list);

                    //表示データをクリアして一時領域のデータをコピー
                    mShowReadData.clear();

                    for(int i = 0 ; i < list.size() ; i++){
                        mShowReadData.add(list.get(i));
                    }
                    mShowReadAdapter.notifyDataSetChanged();
                }
            };
            mViewHandler.post(mViewRunnable);
        }
    }

    private ArrayList<BarcodeInfo> sortBarcodeInfo(ArrayList<BarcodeInfo> list) {
        ArrayList<String> arrayStr = new ArrayList<String>();
        ArrayList<BarcodeInfo> arrayWk = new ArrayList<BarcodeInfo>();
        ArrayList<BarcodeInfo> arraySorted = new ArrayList<BarcodeInfo>();

        //データを抜き出してリスト作成
        for(int i = 0 ; i < list.size() ; i++){
            arrayWk.add(list.get(i));
        }
        //一時領域にコピー
        for(int i = 0 ; i < list.size() ; i++){
            arrayStr.add(list.get(i).getData());
        }

        //データでソート
        Collections.sort(arrayStr);

        //ソート済みリストを使用して検索
        for(int i = 0 ; i < arrayStr.size() ; i++){
            String str = arrayStr.get(i);
            for(int j = 0 ; j < arrayWk.size() ; j++){
                BarcodeInfo info = arrayWk.get(j);
                if(info.getData().equals(str)){
                    //一致したのでBarcodeInfoを戻り値に格納
                    arraySorted.add(info);
                    //戻り値に格納したので削除
                    arrayWk.remove(j);
                    break;
                }
            }
        }
        return arraySorted;
    }

    /**
     * テキスト形式の設定ファイルをSDへ保存
     */
    private void saveSettingText() {
        if (0 == mShowReadData.size()) {
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_savesetting_error), getString(R.string.btn_txt_ok), null,false);
            return;
        }

        // ファイル名入力用エディットテキスト
        final EditText editView = new EditText(ReadBarcodeActivity.this);
        // 現在時刻を取得
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPANESE);
        String fileName = simpleDateFormat.format(new Date(System.currentTimeMillis()));
        // ファイル名にテンプレートをセット
        editView.setText(TEMP_FILENAME + fileName);
        new AlertDialog.Builder(ReadBarcodeActivity.this).setTitle(getString(R.string.title_savedialog))
                // setViewにてビューを設定します。
                .setView(editView).setPositiveButton(getString(R.string.btn_txt_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String filePath = mStoragePath + SAVE_READBARCODE_PATH;
                File file = new File(filePath);
                boolean isCreate = file.mkdirs();
                // ディレクトリがないとき
                if (!isCreate && !file.exists()) {
                    // エラー表示
                    showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null,false);
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
                                showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null,false);
                            }
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            // キャンセルボタン押下
                            return;
                        }
                    },false);
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
        OutputStreamWriter osw = null;
        try {
            // 作成したファイルが PC で見えるように認識させる。
            MediaScannerConnection.scanFile(this, new String[] { filePath }, null, null);

            fos = new FileOutputStream(filePath, true);
            osw = new OutputStreamWriter(fos, "UTF-8");
            bw = new BufferedWriter(osw);
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < mShowReadData.size(); ++i) {
                buf.append(mShowReadData.get(i).getShowData() + COMMA + mShowReadData.get(i).getHexData() + COMMA + mShowReadData.get(i).getType() + NEWLINE);
            }
            String str = buf.toString();

            bw.write(str);
            bw.flush();
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_savefile), getString(R.string.btn_txt_ok), null,false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // エラー表示
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null,false);
        } catch (IOException e) {
            e.printStackTrace();
            // エラー表示
            showDialog(getString(R.string.title_saveresult), getString(R.string.message_saveresult_error), getString(R.string.btn_txt_ok), null,false);

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
    }

    /**
     * バーコードデータアダプタークラス
     */
    private class ShowReadAdapter extends ArrayAdapter<BarcodeInfo> {
        /** バーコードデータ */
        private ArrayList<BarcodeInfo> mItem = null;

        /** コンストラクタ */
        public ShowReadAdapter(Context context, ArrayList<BarcodeInfo> objects) {
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
                Object oTr = getLayoutInflater().inflate(R.layout.barcodelist, null);
                if (oTr instanceof TableRow) {
                    tr = (TableRow) oTr;
                } else {
                    tr = new TableRow(ReadBarcodeActivity.this);
                }
                tr.setBackgroundResource(R.drawable.underline);
            }
            TextView tvCode = null;
            Object oTvCode = tr.findViewById(R.id.textView1);
            if (oTvCode instanceof TextView) {
                tvCode = (TextView) oTvCode;
            } else {
                tvCode = new TextView(ReadBarcodeActivity.this);
            }
            tvCode.setText(mItem.get(position).getShowData());
            TextView tvCodeExt = null;
            Object oTvCodeExt = tr.findViewById(R.id.textView2);
            if (oTvCodeExt instanceof TextView) {
                tvCodeExt = (TextView) oTvCodeExt;
            } else {
                tvCodeExt = new TextView(ReadBarcodeActivity.this);
            }
            tvCodeExt.setText(mItem.get(position).getHexData());

            TextView tvType = null;
            Object oTvType = tr.findViewById(R.id.textView3);
            if (oTvType instanceof TextView) {
                tvType = (TextView)oTvType;
            } else {
                tvType = new TextView(ReadBarcodeActivity.this);
            }
            tvType.setText(mItem.get(position).getType());
            return tr;
        }
    }
    /**
     * 読取タグリストを表示更新
     */
    private class UpdateReadBarcodeDataTask extends AsyncTask<String, String, Long> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Long result) {
            MainMenuActivity.getSDKLibrary().setDataEventEnabled(true);
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mShowReadAdapter.notifyDataSetChanged();
            // 最後の行に移動
            mListView.setSelection(mListView.getCount());
            // 読取数を更新
            mReadCount.setText("" + mShowReadAdapter.getCount());
            super.onProgressUpdate(values);
        }

        @Override
        protected Long doInBackground(String... params) {

            publishProgress("");
            return null;
        }
    }
    /**
     * 読取タグリストを表示更新
     */
    private class CustomUpdateReadBarcodeDataTask extends CustomAsyncTask<String, String, Long> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Long result) {
            MainMenuActivity.getSDKLibrary().setDataEventEnabled(true);
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mShowReadAdapter.notifyDataSetChanged();
            // 最後の行に移動
            mListView.setSelection(mListView.getCount());
            // 読取数を更新
            mReadCount.setText("" + mShowReadAdapter.getCount());
            super.onProgressUpdate(values);
        }

        @Override
        protected Long doInBackground(String... params) {

            publishProgress("");
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    public static class BarcodeInfo {
        private String m_type;
        private String m_length;
        private String m_barcode;
        private String m_showdata;
        private String m_hexdata;

        BarcodeInfo(String type, String length, String barcode){
            this.m_type = type;
            this.m_length = length;
            this.m_barcode = barcode;
            this.m_showdata = convContorolCode(barcode);
            this.m_hexdata = convStr2Hexstr(barcode);
        }

        public String getType(){ return m_type; }
        public String getLength(){ return m_length; }
        public String getData(){ return m_barcode; }
        public String getShowData(){ return m_showdata; }
        public String getHexData(){ return m_hexdata; }

        private final String convStr2Hexstr(String str)
        {
            byte[] bArray = str.getBytes(Charset.forName("UTF-8"));
            //0埋めの16進数に変換
            StringBuilder sb = new StringBuilder(2*bArray.length);
            for(byte b: bArray) {
                sb.append(String.format("%02X", b));
            }
            return String.valueOf(sb);
        }

        private final String convContorolCode(String str)
        {
            byte[] bArray = str.getBytes(Charset.forName("UTF-8"));
            //制御コードは･に変換する
            StringBuilder sb = new StringBuilder(2*bArray.length);
            for(byte b: bArray) {
                if(b<0x1f || b == 0x7f) {
                    sb.append("･");
                }
                else {
                    sb.append(String.format("%c", b));
                }
            }
            return String.valueOf(sb);
        }
    }
}
