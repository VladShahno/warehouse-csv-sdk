package warehouse.com.csv.service.exception;

import lombok.Getter;

@Getter
public class CsvException extends Exception {

  private int line;
  private String content;

  public CsvException(String message) {
    super(message);
  }

  public CsvException(String message, int line, String content) {
    super(message);
    this.line = line;
    this.content = content;
  }

  public CsvException(String message, int line, String content, Throwable cause) {
    super(message, cause);
    this.line = line;
    this.content = content;
  }

  public CsvException(String message, Throwable cause) {
    super(message, cause);
  }
}
