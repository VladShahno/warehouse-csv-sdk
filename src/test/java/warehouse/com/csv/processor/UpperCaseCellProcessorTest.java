package warehouse.com.csv.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpperCaseCellProcessorTest {

  private UpperCaseCellProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new UpperCaseCellProcessor();
  }

  @Test
  void shouldReturnResultInUpperCase() {
    String article = "7z46";
    String result = processor.execute(article, null);

    assertThat(result).isEqualTo("7Z46");
  }

  @Test
  void shouldSkipNullValue() {
    String result = processor.execute(null, null);

    assertThat(result).isNull();
  }
}
