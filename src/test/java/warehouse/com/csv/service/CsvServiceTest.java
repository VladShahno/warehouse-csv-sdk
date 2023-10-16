package warehouse.com.csv.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.prefs.CsvPreference;
import warehouse.com.csv.model.CsvRow;
import warehouse.com.csv.model.TestCsvRow;
import warehouse.com.csv.processor.UpperCaseCellProcessor;
import warehouse.com.csv.service.exception.CsvException;
import warehouse.com.reststarter.exception.BadRequestRestException;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

  private static final List<String> fields_5 = List.of("name", "productGroup", "salePrice",
      "purchasePrice", "article");
  private static final List<String> headers_5 = List.of("Name", "Product Group", "Sale Price",
      "Purchase Price", "Article");
  private static final String headers_5_String = String.join(",", headers_5) + "\n";
  private static final String DATA_WITH_TAB_PREFERENCE = "Name\tProduct Group\tSale Price\n";

  @InjectMocks
  private CsvService csvService;
  @Mock
  private MessageService messageService;

  @Test
  void shouldExportDataWithProcessors() {
    //given
    UpperCaseCellProcessor processor = new UpperCaseCellProcessor();
    TestCsvRow testCsvRow = new TestCsvRow("name", "prts", null, null, null);

    //when
    byte[] bytes = csvService.exportDataWithProcessors(List.of(testCsvRow),
        List.of("Name", "Product Group"),
        List.of("name", "productGroup"), List.of(processor, processor));

    //then
    Assertions.assertEquals("Name,Product Group\r\nNAME,PRTS\r\n", new String(bytes));
  }

  @Test
  void shouldExportData() {
    //given

    //when
    byte[] result = csvService.exportData(emptyList(),
        List.of("Name", "Product Group", "Sale Price"), emptyList());

    //then
    Assertions.assertEquals(31, result.length);
  }

  @Test
  void shouldExportDataToWriter() throws IOException {
    //given
    Writer writer = new StringWriter();

    //when
    csvService.exportDataToWriter(writer, emptyList(),
        List.of("Name", "Product Group", "Sale Price"), emptyList());
    writer.flush();
    writer.close();
    //then
    Assertions.assertEquals(31, writer.toString().getBytes().length);
  }

  @Test
  void shouldExportDataToWriterWithSpecialPreference() {
    //given
    Writer writer = new StringWriter();
    // when
    csvService.exportDataToWriterWithPreference(writer, emptyList(),
        List.of("Name", "Product Group", "Sale Price"),
        emptyList(), CsvPreference.TAB_PREFERENCE);
    //then
    Assertions.assertEquals(DATA_WITH_TAB_PREFERENCE, writer.toString());
  }

  @Test
  void shouldExportDataWithSpecialPreference() {
    //given when
    String result = IOUtils.toString(
        csvService.exportDataWithPreference(emptyList(),
            List.of("Name", "Product Group", "Sale Price"), emptyList(),
            CsvPreference.TAB_PREFERENCE), "UTF-8");

    //then
    Assertions.assertEquals(DATA_WITH_TAB_PREFERENCE, result);
  }

  @Test
  void shouldReadDataFromCsv() {
    //given
    //when
    List<TestCsvRow> result = csvService.readData(headers_5_String + "hod001,7Z46,1234567890,,",
        TestCsvRow.class, headers_5, fields_5);
    //then
    Assertions.assertEquals("hod001", result.get(0).getName());
  }

  @Test
  void shouldReadDataFromCsvWithUtfByteCodeMark() {
    //given
    //when
    List<TestCsvRow> result = csvService.readData(
        "\uFEFFName,Product Group,Sale Price,Purchase Price,Article\n"
            + "hod001,7Z46,1234567890,,", TestCsvRow.class, headers_5, fields_5);
    //then
    Assertions.assertEquals("hod001", result.get(0).getName());
  }

  @Test
  void shouldThrowBadRequestException() {
    //when
    Assertions.assertThrows(BadRequestRestException.class,
        () -> csvService.readData("", TestCsvRow.class, emptyList(), emptyList()));
  }

  @Test
  void shouldThrowWhenReadCsvContentException() {
    //when
    Assertions.assertThrows(BadRequestRestException.class,
        () -> csvService.readData(headers_5_String, TestCsvRow.class, headers_5, fields_5));
  }

  @Test
  void shouldReadDataFromCsvAndModifyToUpperCase() {
    //given
    //when
    List<TestCsvRow> result = csvService.readData(headers_5_String + "hod001,7z46,ek12345678,,",
        TestCsvRow.class, headers_5, fields_5, getCellProcessors());
    //then
    Assertions.assertEquals("7Z46", result.get(0).getProductGroup());
    Assertions.assertEquals("EK12345678", result.get(0).getSalePrice());
  }

  @Test
  void shouldThrowExceptionIfCantMatchCsvWithHeadersOnReadData() {
    //given
    //when
    Assertions.assertThrows(BadRequestRestException.class,
        () -> csvService.readData(headers_5_String + "hod001,7Z46,1234567890,,", TestCsvRow.class,
            headers_5, headers_5));
  }

  @Test
  void shouldReadDataIndexedFromCsv() throws CsvException {
    //given
    //when
    List<TestCsvRow> result = csvService.readIndexedData(
        headers_5_String + "hod001,7Z46,1234567890,,",
        TestCsvRow.class, headers_5, fields_5);
    //then
    Assertions.assertEquals("hod001", result.get(0).getName());
    Assertions.assertEquals(2, result.get(0).getIndex());
  }

  @Test
  void shouldReadDataIndexedFromCsvWithSpecialPreference() throws CsvException {
    //given
    //when
    List<TestCsvRow> result = csvService.readIndexedDataWithSpecialPreference(
        "Name\tProduct Group\tSale Price\tPurchase Price\tArticle\n"
            + "hod001\t7Z46\t1234567890\t\t",
        TestCsvRow.class, headers_5, fields_5, CsvPreference.TAB_PREFERENCE);
    //then
    Assertions.assertEquals("hod001", result.get(0).getName());
    Assertions.assertEquals(2, result.get(0).getIndex());
  }

  @Test
  void shouldReadDataIndexedFromCsvAndModifyToUpperCase() throws CsvException {
    //given
    //when
    List<TestCsvRow> result = csvService.readIndexedData(
        headers_5_String + "hod001,7z46,ek12345678,,",
        TestCsvRow.class, headers_5, fields_5, getCellProcessors());
    //then
    Assertions.assertEquals("7Z46", result.get(0).getProductGroup());
    Assertions.assertEquals("EK12345678", result.get(0).getSalePrice());
  }

  @Test
  void shouldThrowExceptionIfCantMatchCsvWithHeaders() {
    //given
    //when
    Assertions.assertThrows(CsvException.class,
        () -> csvService.readIndexedData(headers_5_String + "hod001,7Z46,1234567890,,",
            TestCsvRow.class, headers_5,
            headers_5));
  }

  @Test
  void shouldThrowExceptionIfTemplateEmpty() {
    //given
    //when

    Assertions.assertThrows(CsvException.class,
        () -> csvService.readIndexedData(headers_5_String, TestCsvRow.class, headers_5, fields_5));
  }

  @ParameterizedTest
  @MethodSource("csvInputEmptyCellsInTheEnd")
  void shouldTrimCsvAndReadData(List<String> headers, List<String> fields, String csvString,
      String sn)
      throws CsvException {
    //given
    //when
    List<TestCsvRow> result = csvService.readIndexedData(csvString, TestCsvRow.class, headers,
        fields);
    //then
    Assertions.assertEquals("VR-S3", result.get(0).getProductGroup());
    Assertions.assertEquals(sn, result.get(0).getSalePrice());
    Assertions.assertEquals(1, result.size());
  }

  @Test
  void shouldNotTrimCsvNotInTheEnd() throws CsvException {
    List<TestCsvRow> result = csvService.readIndexedData(
        headers_5_String + ",,,,\nhod001,7Z46,123,,\n,,,,",
        TestCsvRow.class,
        List.of("Name", "Product Group", "Sale Price", "Purchase Price", "Article"),
        List.of("name", "productGroup", "salePrice", "purchasePrice", "article"));
    //then
    Assertions.assertNull(null, result.get(0).getName());
    Assertions.assertNull(null, result.get(0).getPurchasePrice());
    Assertions.assertEquals("hod001", result.get(1).getName());
    Assertions.assertEquals("123", result.get(1).getSalePrice());
    Assertions.assertEquals(2, result.size());
  }

  @Test
  void shouldExportErrorReportWithAllFields() {
    //given
    HashMap errors = new HashMap<>();
    List<String> errorsList = List.of("salePrice.empty");
    errors.put(1, errorsList);
    when(messageService.getMessages(errorsList)).thenReturn(List.of("Sale Price is Empty"));
    List<CsvRow> entities = new ArrayList<>();
    entities.add(new TestCsvRow("name", "7Z46", "1234567890", "", ""));
    //when
    byte[] result = csvService.exportErrorReport(errors, entities, headers_5);
    //then
    Assertions.assertEquals(78, result.length);
  }

  @Test
  void shouldExportErrorReportWithAllFieldsIfFieldsNotSpecified() {
    //given
    HashMap errors = new HashMap<>();
    List<String> errorsList = List.of("salePrice.empty");
    errors.put(1, errorsList);
    when(messageService.getMessages(errorsList)).thenReturn(List.of("Sale Price is Empty"));
    List<CsvRow> entities = new ArrayList<>();
    entities.add(new TestCsvRow("name", "7Z46", "1234567890", "", ""));
    //when
    byte[] result = csvService.exportErrorReport(errors, entities, headers_5);

    //then
    Assertions.assertEquals(78, result.length);
  }

  @Test
  void shouldExportErrorReportWithSpecifiedFields() {
    //given
    HashMap errors = new HashMap<>();
    List<String> errorsList = List.of("salePrice.empty");
    errors.put(1, errorsList);
    when(messageService.getMessages(errorsList)).thenReturn(List.of("Sale Price is Empty"));
    List<CsvRow> entities = new ArrayList<>();
    entities.add(new TestCsvRow("name", "7Z46", "1234567890", "", ""));
    //when
    byte[] result = csvService.exportErrorReport(errors, entities, List.of("Name", "Product Group"),
        List.of("name", "productGroup"));

    //then
    Assertions.assertEquals(39, result.length);
  }

  @Test
  void shouldExportErrorReportAndRemoveCsvSpecificSymbols() {
    //given
    HashMap errors = new HashMap<>();
    List<String> errorsList = List.of("salePrice.empty");
    errors.put(1, errorsList);
    when(messageService.getMessages(errorsList)).thenReturn(List.of("Sale Price is Empty"));
    List<CsvRow> entities = new ArrayList<>();
    entities.add(new TestCsvRow("name", "=7Z46", "@1234567890", "+1", ""));
    //when
    byte[] result = csvService.exportErrorReport(errors, entities, headers_5);

    //then
    Assertions.assertEquals(79, result.length);
  }

  @Test
  void shouldReadDataToMap() throws Exception {
    //given
    String csvSource =
        """
            ColumnAHeader,ColumnBHeader,ColumnCHeader
            ColumnARow1Value,ColumnBRow1Value,ColumnCRow1Value
            """;
    //when
    List<Map<String, String>> csvRowsData = csvService.readToMap(csvSource,
        CsvPreference.STANDARD_PREFERENCE);
    //then
    Map<String, String> csvRow = csvRowsData.get(0);
    assertThat(csvRow.get("ColumnAHeader")).isEqualTo("ColumnARow1Value");
    assertThat(csvRow.get("ColumnBHeader")).isEqualTo("ColumnBRow1Value");
    assertThat(csvRow.get("ColumnCHeader")).isEqualTo("ColumnCRow1Value");
  }

  @Test
  void shouldThrowExceptionIfHeadersIncorrect() {
    //given
    Assertions.assertThrows(CsvException.class,
        () -> csvService.readIndexedData(headers_5_String + "hod001,7z46,ek12345678,,",
            TestCsvRow.class,
            List.of("Name123", "Product$@#e Group", "Sale453al Price", "Purchase Price", "Article"),
            fields_5,
            getCellProcessors()));
  }

  @Test
  void shouldThrowExceptionIfOneOfTheHeaderIsEmpty() {
    //given
    Assertions.assertThrows(CsvException.class,
        () -> csvService.readIndexedData(
            "Name,Product Group,Sale Price,,Article\n" + "hod001,7Z46,1234567890,,",
            TestCsvRow.class, headers_5, fields_5));
  }

  private CellProcessor[] getCellProcessors() {
    return new CellProcessor[]{
        new org.supercsv.cellprocessor.Optional(),
        new UpperCaseCellProcessor(),
        new UpperCaseCellProcessor(),
        new org.supercsv.cellprocessor.Optional(),
        new org.supercsv.cellprocessor.Optional(),
    };
  }

  private static Stream<Arguments> csvInputEmptyCellsInTheEnd() {
    var fields_3 = List.of("productGroup", "salePrice", "purchasePrice");
    var headers_3 = List.of("Product Group", "Sale Price", "Purchase Price");
    var headers_3_String = String.join(",", headers_3);

    return Stream.of(
        Arguments.of(headers_5, fields_5, headers_5_String + "\n1,VR-S3,111,,\n,", "111"),
        Arguments.of(headers_5, fields_5, headers_5_String + "\n2,VR-S3,222,,\n,  ,", "222"),
        Arguments.of(headers_5, fields_5, headers_5_String + "\n3,VR-S3,333,,\n,  ,,", "333"),
        Arguments.of(headers_5, fields_5, headers_5_String + "\n4,VR-S3,444,,\n,,,,\n", "444"),
        Arguments.of(headers_5, fields_5, headers_5_String + "\n4,VR-S3,555,,\n,  ,\n, ,,,\n",
            "555"),
        Arguments.of(headers_3, fields_3, headers_3_String + "\nVR-S3,111,\n", "111"),
        Arguments.of(headers_3, fields_3, headers_3_String + "\nVR-S3,222,\n,,\n,,", "222"),
        Arguments.of(headers_3, fields_3, headers_3_String + "\nVR-S3,333,\n,,, ,\n", "333"),
        Arguments.of(headers_3, fields_3, headers_3_String + "\nVR-S3,444,\n,, , ,,\n,, ,,,,",
            "444"),
        Arguments.of(headers_3, fields_3,
            headers_3_String + "\nVR-S3,555,\n,, ,,,  ,, ,,, ,,\n,,,,,,", "555"));
  }
}
