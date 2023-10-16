package warehouse.com.csv.service;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_EXPORT_ERROR;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_IMPORT_ERROR;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_IMPORT_INVALID_CONTENT;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.CSV_IMPORT_INVALID_HEADERS;
import static warehouse.com.csv.common.CsvConstants.ErrorKey.EXPORT_ERROR_REPORT_FAILED;

import com.google.common.collect.Iterables;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCsvException;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import warehouse.com.csv.common.EscapeFormulaEncoder;
import warehouse.com.csv.model.CsvRow;
import warehouse.com.csv.service.exception.CsvException;
import warehouse.com.reststarter.exception.BadRequestRestException;
import warehouse.com.reststarter.exception.InternalErrorException;

@Slf4j
public class CsvService {

  private static final int COLUMN_OFFSET = 1;
  // match rows with empty cells and commas after line break
  private static final Pattern emptyCsvRowPattern = Pattern.compile("[\\r\\n]+[,\\s]+");
  private static final CsvPreference CSV_PREFERENCE = new CsvPreference.Builder(
      CsvPreference.STANDARD_PREFERENCE)
      .useEncoder(new EscapeFormulaEncoder())
      .build();
  private final MessageService messageService;

  public CsvService(MessageService messageService) {
    this.messageService = messageService;
  }

  /**
   * Reads a row of a CSV file and populates to the bean.
   *
   * @param csvInput       String with csvInpit
   * @param entityClass    the bean type (the array length should match the number of columns). A
   *                       <tt>null</tt> entry in the array indicates that the column should be
   *                       ignored (the field in the bean will be null - or its default value).
   * @param headers        an array of Strings linking the CSV columns to their corresponding field
   *                       in the bean (each element in the processor array corresponds with a CSV
   *                       column - the number of processor should match the number of columns). A
   *                       <tt>null</tt> entry indicates no further processing is required (the
   *                       unprocessed String value will be set on the bean's field).
   * @param cellProcessors Array of CellProcessors used to further process data before it is
   *                       populated on the bean A <tt>null</tt> indicates no further processing is
   *                       required
   */
  public List readData(
      String csvInput,
      Class entityClass,
      List<String> headers,
      List<String> fieldNames,
      CellProcessor... cellProcessors) {
    try (ICsvBeanReader beanReader = new CsvBeanReader(new StringReader(csvInput),
        CsvPreference.STANDARD_PREFERENCE)) {
      // the header elements are used to map the values to the bean (names must match)
      validateHeaders(beanReader, headers);
      Object entity;
      List importedRows = new ArrayList<>();
      while ((entity = getEntity(beanReader, entityClass, fieldNames, cellProcessors)) != null) {
        importedRows.add(entity);
      }

      if (importedRows.isEmpty()) {
        throw new BadRequestRestException(CSV_IMPORT_ERROR);
      }

      return importedRows;
    } catch (SuperCsvException e) {
      if (e.getCsvContext() != null) {
        log.error(String.format("Failed reading CSV at file row = %s and column = %s",
            e.getCsvContext().getRowNumber(), e.getCsvContext().getColumnNumber()));
      }
      throw new BadRequestRestException(CSV_IMPORT_ERROR, e);
    } catch (Exception e) {
      throw new BadRequestRestException(CSV_IMPORT_ERROR, e);
    }
  }

  /**
   * Reads a row of a CSV file and populates to the indexed bean which extend CsvRow class.
   *
   * @param csvInput       String with csvInput
   * @param entityClass    the bean type (the array length should match the number of columns). A
   *                       <tt>null</tt> entry in the array indicates that the column should be
   *                       ignored (the field in the bean will be null - or its default value).
   * @param headers        an array of Strings linking the CSV columns to their corresponding field
   *                       in the bean (each element in the processor array corresponds with a CSV
   *                       column - the number of processor should match the number of columns). A
   *                       <tt>null</tt> entry indicates no further processing is required (the
   *                       unprocessed String value will be set on the bean's field).
   * @param cellProcessors Array of CellProcessors used to further process data before it is
   *                       populated on the bean A <tt>null</tt> indicates no further processing is
   *                       required
   */
  public <T extends CsvRow> List<T> readIndexedData(
      String csvInput, Class<T> entityClass,
      List<String> headers, List<String> fieldNames, CellProcessor... cellProcessors)
      throws CsvException {
    csvInput = trimCsv(csvInput);
    return readIndexedDataWithSpecialPreference(csvInput, entityClass, headers, fieldNames,
        CsvPreference.STANDARD_PREFERENCE, cellProcessors);
  }

  public <T extends CsvRow> List<T> readIndexedDataWithSpecialPreference(
      String csvInput, Class<T> entityClass,
      List<String> headers, List<String> fieldNames, CsvPreference preference,
      CellProcessor... cellProcessors)
      throws CsvException {
    ICsvBeanReader beanReader = new CsvBeanReader(new StringReader(csvInput), preference);
    try {
      // the header elements are used to map the values to the bean (names must match)
      validateHeaders(beanReader, headers);
      T entity;
      List<T> importedRows = new ArrayList<>();
      while ((entity = getEntity(beanReader, entityClass, fieldNames, cellProcessors)) != null) {
        entity.setIndex(beanReader.getRowNumber());
        importedRows.add(entity);
      }

      if (importedRows.isEmpty()) {
        throw new CsvException("No data was imported");
      }

      return importedRows;
    } catch (RuntimeException e) {
      throw new CsvException(CSV_IMPORT_INVALID_CONTENT, beanReader.getLineNumber(),
          beanReader.getUntokenizedRow(), e);
    } catch (IOException e) {
      throw new CsvException(CSV_IMPORT_ERROR, e);
    }
  }

  private void validateHeaders(ICsvBeanReader beanReader, List<String> headers)
      throws IOException, CsvException {
    String[] headersInFile = beanReader.getHeader(Boolean.TRUE);
    removeUtfByteOrderMark(headersInFile);
    if (!CollectionUtils.isEqualCollection(List.of(headersInFile), headers)) {
      int column = getColumn(List.of(headersInFile), headers);
      throw new CsvException(CSV_IMPORT_INVALID_HEADERS, column + COLUMN_OFFSET,
          headers.get(column));
    }
  }

  private void removeUtfByteOrderMark(String[] headersInFile) {
    if (headersInFile.length > 0 && headersInFile[0].charAt(0) == '\uFEFF') {
      headersInFile[0] = headersInFile[0].substring(1);
    }
  }

  private int getColumn(List<String> headersInFile, List<String> headers) {
    return Iterables.indexOf(headers, header -> !headersInFile.contains(header));
  }

  private <T> T getEntity(
      ICsvBeanReader beanReader,
      Class<T> entityClass,
      List<String> fieldNames,
      CellProcessor[] cellProcessors)
      throws IOException {
    if (ArrayUtils.isNotEmpty(cellProcessors)) {
      return beanReader.read(entityClass, fieldNames.toArray(String[]::new), cellProcessors);
    } else {
      return beanReader.read(entityClass, fieldNames.toArray(String[]::new));
    }
  }

  public List<Map<String, String>> readToMap(String csvInput, CsvPreference csvPreference)
      throws IOException {
    List<Map<String, String>> csvRows = new ArrayList<>();
    try (CsvMapReader mapReader = new CsvMapReader(new StringReader(csvInput), csvPreference)) {
      String[] headers = mapReader.getHeader(true);
      Map<String, String> rowData;
      while ((rowData = mapReader.read(headers)) != null) {
        csvRows.add(rowData);
      }
    }

    return csvRows;
  }

  public byte[] exportDataWithProcessors(
      List entities,
      List<String> headers,
      List<String> fields,
      List<CellProcessor> processors) {
    return exportDataWithPreferenceAndProcessors(entities, headers, fields,
        CsvPreference.STANDARD_PREFERENCE, processors);
  }

  public byte[] exportData(List entities, List<String> headers, List<String> fields) {
    return exportDataWithPreference(entities, headers, fields, CsvPreference.STANDARD_PREFERENCE);
  }

  public byte[] exportDataWithPreference(
      List entities,
      List<String> headers,
      List<String> fields,
      CsvPreference preference) {
    return exportDataWithPreferenceAndProcessors(entities, headers, fields, preference, null);
  }

  public byte[] exportDataWithPreferenceAndProcessors(
      List entities,
      List<String> headers,
      List<String> fields,
      CsvPreference preference,
      List<CellProcessor> cellProcessors) {
    CsvPreference csvPreference = new CsvPreference.Builder(preference).useEncoder(
        new EscapeFormulaEncoder()).build();
    try (Writer writer = new CharArrayWriter();
        ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, csvPreference)) {
      beanWriter.writeHeader(headers.toArray(String[]::new));
      for (Object entity : entities) {
        if (cellProcessors != null) {
          beanWriter.write(entity, fields.toArray(String[]::new),
              cellProcessors.toArray(CellProcessor[]::new));
        } else {
          beanWriter.write(entity, fields.toArray(String[]::new));
        }
      }
      beanWriter.flush();
      return writer.toString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InternalErrorException(CSV_EXPORT_ERROR, e);
    }
  }

  public void exportDataToWriter(Writer writer, List entities, List<String> headers,
      List<String> fields) {
    exportDataToWriterWithPreference(writer, entities, headers, fields,
        CsvPreference.STANDARD_PREFERENCE);
  }

  public void exportDataToWriterWithPreference(
      Writer writer,
      List entities,
      List<String> headers,
      List<String> fields,
      CsvPreference preference) {
    CsvPreference csvPreference = new CsvPreference.Builder(preference).useEncoder(
        new EscapeFormulaEncoder()).build();

    try (ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, csvPreference)) {
      beanWriter.writeHeader(headers.toArray(String[]::new));
      for (Object entity : entities) {
        beanWriter.write(entity, fields.toArray(String[]::new));
      }
      beanWriter.flush();
    } catch (IOException e) {
      throw new InternalErrorException(CSV_EXPORT_ERROR, e);
    }
  }

  public byte[] exportErrorReport(Map<Integer, List<String>> errorsMap, List<CsvRow> entities,
      List<String> headers) {
    return exportErrorReport(errorsMap, entities, headers, Collections.emptyList());
  }

  public byte[] exportErrorReport(Map<Integer, List<String>> errorsMap, List<CsvRow> entities,
      List<String> headers, List<String> fields) {
    Map<Integer, List<String>> messagesMap = convertCodesToMessages(errorsMap);
    List<List<String>> rowsContent = mergeData(entities, fields, messagesMap);

    return exportDataForErrorReport(rowsContent, headers);
  }

  private List<List<String>> mergeData(List<CsvRow> entities, List<String> entityFields,
      Map<Integer, List<String>> errorMessagesMap) {
    List<List<String>> rowsContent = new ArrayList<>();
    entities.forEach(item -> {
      List<String> rowValues = (isNotEmpty(entityFields)) ?
          item.getRowValues(entityFields) :
          item.getRowValues();
      Optional.ofNullable(errorMessagesMap.get(item.getIndex()))
          .ifPresent(rowValues::addAll);
      rowsContent.add(rowValues);
    });

    return rowsContent;
  }

  private Map<Integer, List<String>> convertCodesToMessages(
      Map<Integer, List<String>> errorCodesMap) {
    Map<Integer, List<String>> errorsMessagesMap = new HashMap<>();
    errorCodesMap.forEach((index, errorCodes) -> {
      List<String> messages = messageService.getMessages(errorCodes);
      errorsMessagesMap.put(index, messages);
    });

    return errorsMessagesMap;
  }

  private byte[] exportDataForErrorReport(List<List<String>> rowsContent, List<String> headers) {
    try (Writer writer = new CharArrayWriter();
        ICsvListWriter listWriter = new CsvListWriter(writer, CSV_PREFERENCE)) {
      listWriter.writeHeader(headers.toArray(String[]::new));
      for (List<String> rowValues : rowsContent) {
        listWriter.write(rowValues.toArray(String[]::new));
      }

      listWriter.flush();
      return writer.toString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Failed to write export error report.");
      throw new InternalErrorException(EXPORT_ERROR_REPORT_FAILED, e);
    }
  }

  private String trimCsv(String str) {
    str = StringUtils.trimToEmpty(str);

    var matchedResult = emptyCsvRowPattern
        .matcher(str)
        .results()
        .reduce((first, second) -> second)
        .orElse(null);

    if (Objects.nonNull(matchedResult)) {
      var startMatchedIndex = matchedResult.start();
      var endIndex = matchedResult.end();
      var countMatches = StringUtils.countMatches(matchedResult.group(), ",");

      if (startMatchedIndex > 0 && endIndex == str.length() && countMatches > 0) {
        return str.substring(0, startMatchedIndex + 1);
      }
    }
    return str;
  }
}
