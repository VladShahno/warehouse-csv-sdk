package warehouse.com.csv.common;

import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_EMPTY_FILE_CONTENT;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_IMPORT_ERROR;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_INVALID_FILE_SIZE;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_INVALID_FORMAT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import warehouse.com.reststarter.exception.BadRequestRestException;
import warehouse.com.reststarter.exception.InternalErrorException;

public class CsvUtils {

  private CsvUtils() {
  }

  public static String toString(InputStream inputStream) {
    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
      return scanner.useDelimiter("\\A").next();
    }
  }

  public static void validateCsvFileBody(String body, String errorMessage) {
    if (StringUtils.isBlank(body)) {
      throw new BadRequestRestException(errorMessage);
    }
  }

  public static void validateCsvFileExtension(String fileName, String errorMessage) {
    if (!FilenameUtils.isExtension(fileName, "csv")) {
      throw new BadRequestRestException(errorMessage);
    }
  }

  public static void validateCsvFileSize(Long fileSize, Long maxFileSize) {
    if (fileSize > maxFileSize) {
      throw new BadRequestRestException(CSV_INVALID_FILE_SIZE);
    }
  }

  public static String extractContent(MultipartFile file) {
    try {
      CsvUtils.validateCsvFileExtension(file.getOriginalFilename(), CSV_INVALID_FORMAT);
      String body = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
      CsvUtils.validateCsvFileBody(body, CSV_EMPTY_FILE_CONTENT);

      return body;
    } catch (IOException e) {
      throw new InternalErrorException(CSV_IMPORT_ERROR, e);
    }
  }
}
