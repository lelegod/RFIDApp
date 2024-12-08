package jp.co.toshibatec.uf2200sampleapplication;

import jp.co.toshibatec.uf2200sampleapplication.common.ExcelReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Insets;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.Tag;
import android.os.Build;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

public class PartsFinderActivity extends Activity {
    private ExcelReader excelReader;

    Map<String, ArrayList<String>> parentChildrenMap = new HashMap<>();

    private static PartsFinderActivity mPartsFinderActivity = null;

    public static PartsFinderActivity getInstance() {
        return mPartsFinderActivity;
    }

    /**
     * ダイアログ
     */
    private AlertDialog.Builder mDialog = null;

    /**
     * ダイアログ用ハンドラー
     */
    private Handler mShowDialogHandler = new Handler(Looper.getMainLooper());

    /**
     * ダイアログ用ランナブル
     */
    private Runnable mShowDialogRunnable = null;

    /**
     * 読取テスト中にバックキーが押下されたか
     */
    private boolean isReadBackPress = false;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partsfinder);

        excelReader = new ExcelReader(this);
        Map<String, List<List<String>>> data = excelReader.readExcelFile("RFID_file.xlsx");

        // Sheet2からアイテムコードと品名を取得
        List<List<String>> sheetData = data.get("３．製品構成アプリ");
        if (sheetData != null) {
            for (List<String> row : sheetData) {
                if (row.size() >= 4) {
                    String parentName = row.get(1);
                    String childName = row.get(3);
                    if (!parentChildrenMap.containsKey(parentName)) {
                        parentChildrenMap.put(parentName, new ArrayList<>());
                    }
                    parentChildrenMap.get(parentName).add(childName);
                }
            }
            parentChildrenMap.remove("品名（親）");
        }
        Log.d("parentChildrenMap", parentChildrenMap.toString());

        mPartsFinderActivity = this;
        GridLayout parentGridLayout = findViewById(R.id.parentGrid);
        for (String parentProduct : parentChildrenMap.keySet()) {
            Button button = new Button(this);
            button.setText(parentProduct);

            // Set layout parameters for 2-column layout
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Use weight for equal width
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            button.setOnClickListener(view -> {
                Toast.makeText(this, "Selected: " + parentProduct, Toast.LENGTH_SHORT).show();
            });

            parentGridLayout.addView(button);
        }
    }

    /**
     * エラーダイアログ表示
     *
     * @param title   表示タイトル
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
     * @param title       表示タイトル
     * @param message     表示メッセージ
     * @param btn1Txt     ボタン1
     * @param btn2Txt     ボタン2(不要ならnull)
     * @param positiveRun OKボタン押下
     * @param negativeRun キャンセルボタン押下
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt, final Runnable positiveRun, final Runnable negativeRun) {
        if (null != mShowDialogHandler) {
            mShowDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    mDialog = new AlertDialog.Builder(PartsFinderActivity.this);
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
     * @param title   表示タイトル
     * @param message 表示メッセージ
     * @param btn1Txt ボタン1
     * @param btn2Txt ボタン2(不要ならnull)
     * @param isBack  前の画面に戻るか
     */
    private void showDialog(final String title, final String message, final String btn1Txt, final String btn2Txt, final boolean isBack) {
        if (null != mShowDialogHandler) {
            mShowDialogRunnable = new Runnable() {
                @Override
                public void run() {
                    mDialog = new AlertDialog.Builder(PartsFinderActivity.this);
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
                    if (isBack) {
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
}
