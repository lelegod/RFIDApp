package jp.co.toshibatec.uf2200sampleapplication;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import jp.co.toshibatec.uf2200sampleapplication.common.ExcelReader;

public class PartsFinderChildActivity extends Activity {
    FirebaseFirestore firestore;
    /** 探索対象(EPC) */
    public static final String KEY_TARGET = "target";
    /** EPC指定検索か */
    public static final String KEY_SELECTED_EPC = "isSelectedEPC";
    /** EPC指定済み */
    private boolean isSelectedEPC = true;
    /** 探索対象(EPC) */
    private String searchTarget = null;
    private static PartsFinderChildActivity mPartsFinderChildActivity = null;
    /** 接続要求MACアドレス */
    private String mConnectionRequestString = null;

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

    private String source = "child";
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.partsfinder_child);
        if (getIntent().hasExtra("EXTRA_CONNECTION_REQUEST")) {
            mConnectionRequestString = getIntent().getStringExtra("EXTRA_CONNECTION_REQUEST");
        }
        if(getActionBar()!=null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mPartsFinderChildActivity = this;
        firestore = FirebaseFirestore.getInstance();

        ArrayList<String> childProducts = getIntent().getStringArrayListExtra("childElements");

        GridLayout childGridLayout = findViewById(R.id.childGrid);
        assert childProducts != null;
        for (String childProduct : childProducts) {
            Button button = new Button(this);
            button.setText(childProduct);
            button.setBackgroundColor(Color.parseColor("#23a9a9"));
            button.setTextColor(Color.WHITE);
            button.setBackground(createButtonBackground());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            // Set onClickListener for each child button
            button.setOnClickListener(view -> {
//                Toast.makeText(this, "Selected Child: " + child, Toast.LENGTH_SHORT).show();
                Log.d("parentChildrenMap", childProduct.toString());
                firestore.collection("tag_list")
                        .whereEqualTo("product_name", childProduct)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                CollectionReference tag_list = firestore.collection("tag_list");
                                if (task.isSuccessful()) {
                                    if (task.getResult().isEmpty()) {
                                        showDialog(getString(R.string.title_error), getString(R.string.message_check_error), getString(R.string.btn_txt_ok), null);
                                    } else {
                                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                        searchTarget = document.getString("epc_code");
                                        Log.d("parentChildrenMap", searchTarget.toString());
                                        Intent intent = new Intent(view.getContext(), jp.co.toshibatec.searchsampletool.MainActivity.class);
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            intent.putExtra("EXTRA_CONNECTION_REQUEST", mConnectionRequestString);
                                            intent.putExtra("SOURCE", source);
                                            intent.putExtra(KEY_TARGET, searchTarget);
                                            Log.d("parentChildrenMap", searchTarget.toString());
                                            intent.putExtra(KEY_SELECTED_EPC, isSelectedEPC);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(view.getContext(), "Activity not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } else {
                                    // Handle failure of the query
                                    showDialog(getString(R.string.title_error), getString(R.string.message_check_error), getString(R.string.btn_txt_ok), null);
                                }
                            }
                        });
            });
            childGridLayout.addView(button);
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
            intent.setClassName(this, "jp.co.toshibatec.uf2200sampleapplication.PartsFinderActivity");
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

