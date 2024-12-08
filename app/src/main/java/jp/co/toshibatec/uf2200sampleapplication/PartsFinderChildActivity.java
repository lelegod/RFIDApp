package jp.co.toshibatec.uf2200sampleapplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import jp.co.toshibatec.uf2200sampleapplication.common.ExcelReader;

public class PartsFinderChildActivity extends Activity {
    private static PartsFinderChildActivity mPartsFinderChildActivity = null;

    public static PartsFinderChildActivity getInstance() {
        return mPartsFinderChildActivity;
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
        setContentView(R.layout.partsfinder_child);
        mPartsFinderChildActivity = this;

        ArrayList<String> childProducts = getIntent().getStringArrayListExtra("childElements");

        GridLayout childGridLayout = findViewById(R.id.childGrid);
        assert childProducts != null;
        for (String child : childProducts) {
            Button button = new Button(this);
            button.setText(child);
            button.setBackgroundColor(Color.parseColor("#23a9a9"));
            button.setTextColor(Color.WHITE);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            // Set onClickListener for each child button
            button.setOnClickListener(view -> {
//                Toast.makeText(this, "Selected Child: " + child, Toast.LENGTH_SHORT).show();
            });

            childGridLayout.addView(button);
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
                    mDialog = new AlertDialog.Builder(PartsFinderChildActivity.this);
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
                    mDialog = new AlertDialog.Builder(PartsFinderChildActivity.this);
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

