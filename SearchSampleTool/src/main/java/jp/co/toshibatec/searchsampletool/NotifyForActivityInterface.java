
package jp.co.toshibatec.searchsampletool;

public interface NotifyForActivityInterface {

    /** デバイス切断検知を通知する */
    public void disconnectDevice(String title, String message, String btn1);

    /** デバイス再接続成功通知をする */
    public void reConnectDeviceSuccess();

    /** デバイス再接続失敗通知をする */
    public void reConnectDeviceFailed();
}
