package warehouse.com.csv.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public abstract class CsvRow {

  private int index;

  //override this method to return list of entity fields for csv row.
  public List<String> getRowValues() {
    return new ArrayList<>();
  }

  public List<String> getRowValues(List<String> fields) {
    return new ArrayList<>();
  }

}
