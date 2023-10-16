package warehouse.com.csv.model;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class TestCsvRow extends CsvRow {

  private String name;

  private String productGroup;

  private String purchasePrice;

  private String salePrice;

  private String article;

  @Override
  public List<String> getRowValues() {
    List<String> rowValues = new ArrayList<>();
    rowValues.add(getNameRowValue());
    rowValues.add(getProductGroupRowValue());
    rowValues.add(getPurchasePriceRowValue());
    rowValues.add(getSalePriceRowValue());
    rowValues.add(getArticleRowValue());

    return rowValues;
  }

  @Override
  public List<String> getRowValues(List<String> fields) {
    List<String> rowValues = new ArrayList<>();
    if (fields.contains(Fields.name)) {
      rowValues.add(trimToEmpty(name));
    }
    if (fields.contains(Fields.productGroup)) {
      rowValues.add(trimToEmpty(Fields.productGroup));
    }
    if (fields.contains(Fields.purchasePrice)) {
      rowValues.add(trimToEmpty(Fields.purchasePrice));
    }
    if (fields.contains(Fields.salePrice)) {
      rowValues.add(trimToEmpty(Fields.salePrice));
    }
    if (fields.contains(Fields.article)) {
      rowValues.add(trimToEmpty(Fields.article));
    }

    return rowValues;
  }

  private String getNameRowValue() {
    return trimToEmpty(name);
  }

  private String getProductGroupRowValue() {
    return trimToEmpty(productGroup);
  }

  private String getPurchasePriceRowValue() {
    return trimToEmpty(purchasePrice);
  }

  private String getSalePriceRowValue() {
    return trimToEmpty(salePrice);
  }

  private String getArticleRowValue() {
    return trimToEmpty(article);
  }
}

