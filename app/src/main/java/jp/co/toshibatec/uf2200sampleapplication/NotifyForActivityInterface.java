
package jp.co.toshibatec.uf2200sampleapplication;

/**
 * 通知のためのインターフェイス
 * 
 * @author 003275
 */
public interface NotifyForActivityInterface {

    /** デバイス切断検知を通知する */
    public void disconnectDevice(String title, String message, String btn1);

}
