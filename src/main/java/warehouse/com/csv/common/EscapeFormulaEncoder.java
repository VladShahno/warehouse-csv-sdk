package warehouse.com.csv.common;

import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

public class EscapeFormulaEncoder extends DefaultCsvEncoder {

  @Override
  public String encode(final String input, final CsvContext context,
      final CsvPreference preference) {
    return super.encode(input, context, preference).replaceFirst("^[=+\\-@]+", "")
        .replaceFirst("^\"[=+\\-@]+", "\"");
  }
}
