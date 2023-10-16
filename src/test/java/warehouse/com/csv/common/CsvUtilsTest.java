package warehouse.com.csv.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_EMPTY_FILE_CONTENT;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_IMPORT_ERROR;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_INVALID_FILE_SIZE;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_INVALID_FORMAT;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import warehouse.com.reststarter.exception.BadRequestRestException;
import warehouse.com.reststarter.exception.InternalErrorException;

@ExtendWith(MockitoExtension.class)
class CsvUtilsTest {

  @Test
  void shouldFailIfFileNotCsv() throws IOException {
    //given
    String testCaseFileName = "import_fail_file.txt";
    MockMultipartFile testFile = new MockMultipartFile(testCaseFileName, testCaseFileName,
        MULTIPART_FORM_DATA_VALUE,
        this.getClass().getClassLoader().getResourceAsStream("samples/" + testCaseFileName));

    //when then
    Throwable exception = assertThrows(BadRequestRestException.class,
        () -> CsvUtils.extractContent(testFile));
    assertTrue(exception.getMessage().contains(CSV_INVALID_FORMAT));
  }

  @Test
  void shouldFailIfFileIsEmpty() throws IOException {
    //given
    String testCaseFileName = "import_empty_body.csv";
    MockMultipartFile testFile = new MockMultipartFile(testCaseFileName, testCaseFileName,
        MULTIPART_FORM_DATA_VALUE,
        this.getClass().getClassLoader().getResourceAsStream("samples/" + testCaseFileName));

    //when then
    Throwable exception = assertThrows(BadRequestRestException.class,
        () -> CsvUtils.extractContent(testFile));
    assertTrue(exception.getMessage().contains(CSV_EMPTY_FILE_CONTENT));
  }

  @Test
  void shouldThrowInternalErrorIfIOException() throws IOException {
    //given
    MockMultipartFile testFile = mock(MockMultipartFile.class);
    when(testFile.getOriginalFilename()).thenReturn("Product.csv");
    when(testFile.getInputStream()).thenThrow(new IOException());

    //when then
    Throwable exception = assertThrows(InternalErrorException.class,
        () -> CsvUtils.extractContent(testFile));
    assertTrue(exception.getMessage().contains(CSV_IMPORT_ERROR));
  }

  @Test
  void shouldExtractStringFromCsv() throws IOException {
    //given
    String testCaseFileName = "product.csv";
    MockMultipartFile testFile = new MockMultipartFile(testCaseFileName, testCaseFileName,
        MULTIPART_FORM_DATA_VALUE,
        this.getClass().getClassLoader().getResourceAsStream("samples/" + testCaseFileName));

    //when
    String result = CsvUtils.extractContent(testFile);

    //then
    assertEquals("Name,Product Group\n" +
        "Wheel,AutoParts\n", result);
  }

  @Test
  void shouldReadStringFromBytes() {
    //given
    String testCaseFileName = "product.csv";
    //when
    String result = CsvUtils.toString(
        this.getClass().getClassLoader().getResourceAsStream("samples/" + testCaseFileName));
    //then
    assertEquals("Name,Product Group\n" +
        "Wheel,AutoParts\n", result);
  }

  @Test
  void shouldDoNothingIfFileDoesNotExceedsLimit() {
    //given
    Long fileSize = 1000L;
    Long maxFileSize = 1000L;

    //when then
    CsvUtils.validateCsvFileSize(fileSize, maxFileSize);
  }

  @Test
  void shouldThrowExceptionIfFileExceedsLimit() {
    //given
    Long fileSize = 2000L;
    Long maxFileSize = 1000L;

    //when then
    Throwable exception = assertThrows(BadRequestRestException.class,
        () -> CsvUtils.validateCsvFileSize(fileSize, maxFileSize));
    assertTrue(exception.getMessage().contains(CSV_INVALID_FILE_SIZE));
  }
}
