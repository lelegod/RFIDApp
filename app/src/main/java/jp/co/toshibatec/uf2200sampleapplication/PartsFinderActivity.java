package jp.co.toshibatec.uf2200sampleapplication;

import jp.co.toshibatec.uf2200sampleapplication.common.ExcelReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class PartsFinderActivity extends Activity {
    private ExcelReader excelReader;

    Map<String, ArrayList<String>> parentChildrenMap = new HashMap<>();

    private static PartsFinderActivity mPartsFinderActivity = null;

    public static PartsFinderActivity getInstance() {
        return mPartsFinderActivity;
    }
    /** 接続要求MACアドレス */
    private String mConnectionRequestString = null;
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
        if (getIntent().hasExtra("EXTRA_CONNECTION_REQUEST")) {
            String mConnectionRequestString = getIntent().getStringExtra("EXTRA_CONNECTION_REQUEST");
        }
        if(getActionBar()!=null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
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
            button.setBackgroundColor(Color.parseColor("#23a9a9"));
            button.setTextColor(Color.WHITE);
            button.setBackground(createButtonBackground());
            // Set layout parameters for 2-column layout
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // Use weight for equal width
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            button.setOnClickListener(view -> {
//                Toast.makeText(this, "Selected: " + parentProduct, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, PartsFinderChildActivity.class);
                intent.putExtra("EXTRA_CONNECTION_REQUEST", mConnectionRequestString);
                intent.putStringArrayListExtra("childElements", parentChildrenMap.get(parentProduct));
                startActivity(intent);
            });

            parentGridLayout.addView(button);
        }
    }
    private StateListDrawable createButtonBackground() {
        // Default state (normal)
        GradientDrawable normalDrawable = new GradientDrawable();
        normalDrawable.setColor(Color.parseColor("#23a9a9"));
        normalDrawable.setCornerRadius(16); // Rounded corners

        // Pressed state
        GradientDrawable pressedDrawable = new GradientDrawable();
        pressedDrawable.setColor(Color.parseColor("#1e8888"));
        pressedDrawable.setCornerRadius(16); // Rounded corners

        // StateListDrawable to manage states
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        stateListDrawable.addState(new int[]{}, normalDrawable); // Default state

        return stateListDrawable;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent();
            intent.setClassName(this, "jp.co.toshibatec.uf2200sampleapplication.MainMenuActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
