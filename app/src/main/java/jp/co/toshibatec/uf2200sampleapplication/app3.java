package jp.co.toshibatec.uf2200sampleapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class app3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productassignment);

        // ボタンを配置するレイアウトを取得
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);

        // エクセルから親子データを取得
        HashMap<String, List<String>> parentToChildrenMap = readExcelData();

        // 動的にボタンを生成
        for (String parent : parentToChildrenMap.keySet()) {
            Button button = new Button(this);
            button.setText(parent);

            // スコープ内で親を固定
            String finalParent = parent;

            button.setOnClickListener(view -> {
                List<String> children = parentToChildrenMap.get(finalParent);
                if (children != null && !children.isEmpty()) {
                    displayChildren(children);
                } else {
                    Toast.makeText(this, "子がありません", Toast.LENGTH_SHORT).show();
                }
            });

            button.setPadding(20, 20, 20, 20);
            buttonContainer.addView(button);
        }
    }

    // エクセルファイルを読み込むメソッド
    private HashMap<String, List<String>> readExcelData() {
        HashMap<String, List<String>> parentToChildrenMap = new HashMap<>();

        try {
            // assetsフォルダからエクセルファイルを読み込む
            InputStream inputStream = getAssets().open("app3.xlsx");
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // 最初のシートを取得

            for (Row row : sheet) {
                Cell parentCell = row.getCell(0); // A列: 親
                Cell childCell = row.getCell(1);  // B列: 子

                if (parentCell != null && childCell != null) {
                    String parent = parentCell.getStringCellValue().trim();
                    String child = childCell.getStringCellValue().trim();

                    if (!parent.isEmpty() && !child.isEmpty()) {
                        parentToChildrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
                    }
                }
            }

            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "エクセルデータの読み込みに失敗しました", Toast.LENGTH_SHORT).show();
        }

        return parentToChildrenMap;
    }

    // 子を表示するメソッド
    private void displayChildren(List<String> children) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("品名（子）");
        builder.setItems(children.toArray(new String[0]), null);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}
