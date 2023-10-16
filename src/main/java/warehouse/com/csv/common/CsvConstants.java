package warehouse.com.csv.common;

public class CsvConstants {

  private CsvConstants() {
  }

  public static class ErrorKey {

    public static final String CSV_EXPORT_ERROR = "csv.export.error";
    public static final String CSV_IMPORT_ERROR = "csv.import.error";
    public static final String CSV_INVALID_FORMAT = "csv.invalid.format";
    public static final String CSV_INVALID_FILE_SIZE = "csv.invalid.file.size";
    public static final String CSV_EMPTY_FILE_CONTENT = "csv.empty.file.content";
    public static final String EXPORT_ERROR_REPORT_FAILED = "export.error.report.failed";
    public static final String CSV_IMPORT_INVALID_CONTENT = "csv.import.invalid.content";
    public static final String CSV_IMPORT_INVALID_HEADERS = "csv.import.invalid.headers";
    private ErrorKey() {
    }
  }
}
