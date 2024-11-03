package jp.co.toshibatec.uf2200sampleapplication.common;
import android.content.Context;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {

    private Context context;

    public ExcelReader(Context context) {
        this.context = context;
    }

    public Map<String, List<List<String>>> readExcelFile(String fileName) {
        Map<String, List<List<String>>> data = new HashMap<>();
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            Workbook workbook = WorkbookFactory.create(inputStream);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = new ArrayList<>();

                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowData.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                rowData.add(String.valueOf(cell.getNumericCellValue()));
                                break;
                            case BOOLEAN:
                                rowData.add(String.valueOf(cell.getBooleanCellValue()));
                                break;
                            case FORMULA:
                                switch (cell.getCachedFormulaResultType()) {
                                    case STRING:
                                        rowData.add(cell.getStringCellValue());
                                        break;
                                    case NUMERIC:
                                        rowData.add(String.valueOf(cell.getNumericCellValue()));
                                        break;
                                    case BOOLEAN:
                                        rowData.add(String.valueOf(cell.getBooleanCellValue()));
                                        break;
                                    default:
                                        rowData.add(cell.getCellFormula());
                                        break;
                                }
                                break;
                            default:
                                rowData.add(cell.toString());
                                break;
                        }
                    }
                    sheetData.add(rowData);
                }
                data.put(sheet.getSheetName(), sheetData); // シート名をキーにしてデータを保存
            }

            workbook.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data; // シート名とデータのマップを返す
    }
}

