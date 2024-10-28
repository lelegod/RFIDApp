package jp.co.toshibatec.searchsampletool;

import java.util.ArrayList;

import jp.co.explorationrfid.DeviceSensorListener;
import jp.co.explorationrfid.ExplorationRfid;
import jp.co.explorationrfid.PickRadarView;
import jp.co.explorationrfid.PositionMoveEventListener;
import jp.co.explorationrfid.RssiEventListener;
import jp.co.explorationrfid.callback.EPCDataEvent;
import jp.co.explorationrfid.callback.FinCallback;
import jp.co.explorationrfid.callback.SettingEndEvent;
import jp.co.explorationrfid.callback.TriggerOffEvent;
import jp.co.explorationrfid.callback.TriggerOnEvent;
import jp.co.toshibatec.TecRfidSuite;
import jp.co.toshibatec.searchsampletool.log.Log;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SearchActivity extends LibAccessBaseActivity implements View.OnClickListener, PositionMoveEventListener,
        DeviceSensorListener, RssiEventListener, NotifyForActivityInterface {
    /** 探索開始ボタン */
    private ImageView mSearchStartBtn = null;
    /** 探索停止ボタン */
    private ImageView mSearchStopBtn = null;
    /** EPC */
    private TextView mEpcText = null;
    /** レーダー画面 */
    private jp.co.explorationrfid.PickRadarView mPickRadarView = null;
    /** サーフェスビュー */
    private SurfaceView mRadarSurfaceView = null;
    /** ExplorationRfidライブラリ */
    private ExplorationRfid explorationRfid;
    /** TecRfidSuiteライブラリ */
    private TecRfidSuite mLib = null;
    /** スタート出力 */
    private int handyPower = 19;
    /** エンド出力 */
    private int findPower = 9;
    /** 絶対位置 */
    private float absoluteAngle = 0;
    /** 出力割合 */
    private float power = 0;
    /** 探索開始 */
    private boolean isStartSearch = false;
    /** PositionMoveEvent開始フラグ */
    private boolean startPositionMoveEvent = false;
    /** 探索対象(EPC) */
    private String searchTarget = null;
    /** ハンドラー */
    private Handler handler = new Handler(Looper.getMainLooper());
    /** EPC指定済み */
    private boolean isSelectedEPC = false;
    /** 探索対象指定リスト */
    private ArrayList<String> mEpcCodeList = null;
    /** 除外対象リスト */
    private ArrayList<String> mExclusionList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.info(START);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        //前画面から渡された情報を反映
        Intent intent = getIntent();
        if (intent != null) {
            isSelectedEPC = intent.getBooleanExtra(KEY_SELECTED_EPC,false);
            if(isSelectedEPC) {
                searchTarget = intent.getStringExtra(KEY_TARGET);
            }
            else {
                mEpcCodeList = intent.getStringArrayListExtra(KEY_EPCLIST);
                mExclusionList = intent.getStringArrayListExtra(KEY_EXCLUSIONLIST);
            }
        }
        // リスナーを登録
        MainActivity.setListener(this);
        // EPCテキスト設定
        Object omEpcText = findViewById(R.id.epc_text);
        if (omEpcText instanceof TextView) {
            mEpcText = (TextView) omEpcText;
        } else {
            mEpcText = new TextView(SearchActivity.this);
        }
        mEpcText.setText(searchTarget);
        // 探索開始ボタン設定
        Object omSearchStartBtn = findViewById(R.id.searchstart_btn);
        if (omSearchStartBtn instanceof ImageView) {
            mSearchStartBtn = (ImageView) omSearchStartBtn;
        } else {
            mSearchStartBtn = new ImageView(SearchActivity.this);
        }
        mSearchStartBtn.setOnClickListener(this);
        // 探索停止ボタン設定
        Object omSearchStopBtn = findViewById(R.id.searchstop_btn);
        if (omSearchStopBtn instanceof ImageView) {
            mSearchStopBtn = (ImageView) omSearchStopBtn;
        } else {
            mSearchStopBtn = new ImageView(SearchActivity.this);
        }
        mSearchStopBtn.setOnClickListener(this);

        // 描画用SurfaceView設定
        Object omRadarSurfaceView = findViewById(R.id.radar_surfaceView);
        if (omRadarSurfaceView instanceof SurfaceView) {
            mRadarSurfaceView = (SurfaceView) omRadarSurfaceView;
        }

        // 探索ライブラリ設定
        settingFixFinderLiblary();
        Log.info(END);
    }

    @Override
    public void onClick(View v) {
        Log.info(START);
        if (v.equals(mSearchStartBtn)) {
            if (!(MainActivity.getInstance()).getDisconnectFlag()) {
                if (!isStartSearch) {
                    isStartSearch = true;
                } else {
                    return;
                }
                showProgress();
                // リスナーを追加する
                explorationRfid.setPositionMoveListener(this);
                explorationRfid.setDeviceSensorListener(this);
                explorationRfid.setRssiListener(this);
                // レーダークリア
                mPickRadarView.clearRadar();
                // オフセット[bit]
                int offset = OFFSET_SIZE;
                if (isSelectedEPC) {
                    // EPC指定済み
                    searchStartWithCallback(offset);
                } else {
                    // EPCコードのマスクサイズ[bit]
                    int selectSize = SELECT_SIZE;
                    mEpcText.setText("");
                    // 探索対象指定
                    searchStartWithReadingTagCallback(mEpcCodeList, offset, selectSize, mExclusionList);
                }
                mPickRadarView.startRadar();

            } else {
                MainActivity.getInstance().deviceReConnect(this);
            }
        } else if (v.equals(mSearchStopBtn)) {
            if (!(MainActivity.getInstance()).getDisconnectFlag()) {
                if (!isStartSearch) {
                    return;
                }
                showProgress();
                mPickRadarView.stopRadar();
                searchStopWithCallback();
            } else {
                MainActivity.getInstance().deviceReConnect(this);
            }
        }
    }

    private void settingFixFinderLiblary() {
        // TecRfidSuiteライブラリインスタンスを取得
        mLib = TecRfidSuite.getInstance();
        explorationRfid = new ExplorationRfid(getApplicationContext());
        explorationRfid.logLevel = 2;
        explorationRfid.logEnableConsol = true;
        explorationRfid.logEnableFile = true;
        explorationRfid.setLog();
        explorationRfid.continuitySuccessCount = 1;
        explorationRfid.continuityEndSuccessCount = 3;
        explorationRfid.continuityLostCount = 3;
        explorationRfid.limitRiseIncrement = 3;
        explorationRfid.readTimerInterval = 100;
        explorationRfid.volume = 0.4f;

        // PickRadarViewインスタンスを生成
        String packageName = getPackageName();
        mPickRadarView = new PickRadarView(this, mRadarSurfaceView, packageName);

        // 設定する音楽ファイル
        String SoundFileName = "android.resource://" + getPackageName() + "/";
        ArrayList<Uri> list = new ArrayList<Uri>();
        list.add(0, Uri.parse(SoundFileName + R.raw.rssisound3));
        list.add(1, Uri.parse(SoundFileName + R.raw.rssisound2));
        list.add(2, Uri.parse(SoundFileName + R.raw.rssisound1));
        if (!explorationRfid.setSoundFile(list)) {
            showDialog(getString(R.string.title_error), "音楽ファイルの最大設定数を超えています", getString(R.string.btn_txt_ok), null);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE);
        int radarDrawMode =prefs.getInt(SEARCH_RADAR_DRAW_MODE, 1);
        if(radarDrawMode ==1) {
            mPickRadarView.radarDrawMode =true;
        }
        else {
            mPickRadarView.radarDrawMode =false;
        }
        explorationRfid.fwMode =prefs.getInt(SEARCH_FW_MODE, 1);
        Log.info("fwMode="+explorationRfid.fwMode);
        explorationRfid.sensorMode = 0;
    }

    /**
     * EPC指定検索
     */
    private void searchStartWithCallback(int offset) {
        Log.info("searchStartWithCallback");
        int ret = explorationRfid.searchStartWithCallback(mLib, offset, searchTarget, handyPower, findPower, new FinCallback() {

            @Override
            public void finCallback(int resultCode, Error error) {
                if ((MainActivity.getInstance()).getDisconnectFlag()) {
                    return;
                }
                dismissProgress();
                if (resultCode != TecRfidSuite.OPOS_SUCCESS) {
                    isStartSearch = false;
                    showDialog(getString(R.string.title_error), error.getMessage(), getString(R.string.btn_txt_ok),
                            null);
                }
            }

        }, new TriggerOnEvent() {

            @Override
            public void triggerOnEvent() {
                Log.info("triggerOnEvent");
            }

        }, new TriggerOffEvent() {

            @Override
            public void triggerOffEvent() {
                Log.info("triggerOffEvent");
            }
        });
        if (ret != TecRfidSuite.OPOS_SUCCESS) {
            dismissProgress();
            isStartSearch = false;
            showDialog(getString(R.string.title_error), "searchStartWithCallback失敗　ret = "+ret, getString(R.string.btn_txt_ok),
                    null);
        }
    }

    /**
     * フィルタ指定検索
     */
    private void searchStartWithReadingTagCallback(ArrayList<String> partNumberList, int offset, int selectSize,
                                                   ArrayList<String> exclusionList) {
        Log.debug("searchStartWithReadingTagCallback");
        int ret = explorationRfid.searchStartWithReadingTagCallback(mLib, partNumberList, offset, selectSize,
                exclusionList, handyPower, findPower, new SettingEndEvent() {

                    @Override
                    public void settingEndEvent() {
                        Log.info("settingEndEvent");
                        dismissProgress();
                    }
                }, new EPCDataEvent() {

                    @Override
                    public void dataEvent(final String resultEpcCode) {
                        Log.info("dataEvent resultEpcCode="+resultEpcCode);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (resultEpcCode != null) {
                                    mEpcText.setText(resultEpcCode);
                                    Log.info("特定したEPC:" + resultEpcCode);
                                }
                            }
                        });
                    }
                }, new FinCallback() {

                    @Override
                    public void finCallback(int resultCode, Error error) {
                        Log.info("finCallback  resultCode= "+resultCode);
                        if (resultCode != TecRfidSuite.OPOS_SUCCESS) {
                            dismissProgress();
                            isStartSearch = false;
                            showDialog(getString(R.string.title_error), error.getMessage(),
                                        getString(R.string.btn_txt_ok), null);
                        }
                    }

                }, new TriggerOnEvent() {

                    @Override
                    public void triggerOnEvent() {
                        Log.info("triggerOnEvent");
                    }

                }, new TriggerOffEvent() {

                    @Override
                    public void triggerOffEvent() {
                        Log.info("triggerOffEvent");
                    }
                });
        Log.info("ret = " + ret);
        if(ret!=TecRfidSuite.OPOS_SUCCESS) {
            startPositionMoveEvent = false;
            isStartSearch = false;
            dismissProgress();
            showDialog(getString(R.string.title_error), "searchStartWithReadingTagCallback失敗　ret = "+ret, getString(R.string.btn_txt_ok),
                    null);
        }
    }


    /**
     * 探索停止
     */
    private void searchStopWithCallback() {
        Log.info("searchStopWithCallback");
        int ret = explorationRfid.searchStopWithCallback(new FinCallback() {

            @Override
            public void finCallback(int resultCode, Error error) {
                startPositionMoveEvent = false;
                isStartSearch = false;
                dismissProgress();
            }
        });
        if (ret != TecRfidSuite.OPOS_SUCCESS) {
            showDialog(getString(R.string.title_error), "searchStopWithCallback失敗　ret = "+ret, getString(R.string.btn_txt_ok),
                    null);
        }
    }

    /**
     * rssiEvent
     */
    @Override
    public void rssiEvent(int rssi) {
        Log.info("rssiEvent");
        if ((MainActivity.getInstance()).getDisconnectFlag()) {
            return;
        }
        mPickRadarView.drawRssi(rssi);
    }

    /**
     * sensorEvent
     */
    @Override
    public void deviceSensorEvent(float realAngle) {
        Log.info("deviceSensorEvent");
        if ((MainActivity.getInstance()).getDisconnectFlag()) {
            return;
        }
        if (startPositionMoveEvent) {
            mPickRadarView.drawRadar(absoluteAngle, power, realAngle);
        }
    }

    /**
     * PositionMoveEvent
     */
    @Override
    public void positionMoveEvent(float absoluteAngle, float power, float realAngle) {
        Log.info("positionMoveEvent");
        if ((MainActivity.getInstance()).getDisconnectFlag()) {
            return;
        }
        this.absoluteAngle = calcMidPoint(this.absoluteAngle,absoluteAngle);
        this.power = power;
        if (!startPositionMoveEvent) {
            startPositionMoveEvent = true;
        }
        mPickRadarView.drawRadar(absoluteAngle, power, realAngle);
    }

    private float calcMidPoint(float before,float after) {
        float ret = 0.0f;
        if ((before > 0 && after > 0) || (before < 0 && after < 0)) {
            ret = (before + after) / 2;
        } else {
            if (Math.abs(before) + Math.abs(after) > 180) {
                float midpoint = (before + after) / 2;
                if (midpoint > 0) {
                    ret = midpoint - 180;
                } else {
                    ret = midpoint + 180;
                }
            } else {
                ret = (before + after) / 2;
            }
        }
        return ret;
    }

    @Override
    public void disconnectDevice(String title, String message, String btn1) {
        Log.info("disconnectDevice");
        dismissProgress();
        showDialog(title, message, btn1, null);
    }

    @Override
    public void reConnectDeviceSuccess() {
        Log.info("reConnectDeviceSuccess");
        showDialog(null, "reConnectDeviceSuccess", getString(R.string.btn_txt_ok), null);
    }

    @Override
    public void reConnectDeviceFailed() {
        Log.info("reConnectDeviceFailed");
        // エラー表示
        showDialog(getString(R.string.title_error), getString(R.string.message_reconnect_failed),
                getString(R.string.btn_txt_ok), null);

    }

    @Override
    protected void onDestroy() {
        Log.info(START);
        explorationRfid = null;
        mLib = null;
        Log.info(END);
        super.onDestroy();
    }
}
