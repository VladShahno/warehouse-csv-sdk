package warehouse.com.csv.processor;

import java.util.Objects;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.util.CsvContext;

public class UpperCaseCellProcessor extends CellProcessorAdaptor implements StringCellProcessor {

  @Override
  public <T> T execute(Object value, CsvContext context) {
    if (Objects.nonNull(value)) {
      final String result = value.toString().toUpperCase();
      return next.execute(result, context);
    }
    return next.execute(value, context);
  }
}
