package warehouse.com.csv.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static warehouse.com.csv.TestConstants.MESSAGE_CODE;
import static warehouse.com.csv.TestConstants.MESSAGE_FROM_PROPERTIES;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @InjectMocks
  private MessageService messageService;
  @Mock
  private MessageSource messageSource;

  @Test
  void shouldGetMessageFromSource() {
    //given
    when(messageSource.getMessage(eq(MESSAGE_CODE), any(), eq(MESSAGE_CODE),
        eq(Locale.getDefault())))
        .thenReturn(MESSAGE_FROM_PROPERTIES);

    //when
    String result = messageService.getMessage(MESSAGE_CODE, null);

    //then
    Assertions.assertEquals(MESSAGE_FROM_PROPERTIES, result);
  }

  @Test
  void shouldGetMessagesFromSource() {
    //given
    when(messageSource.getMessage(eq(MESSAGE_CODE), any(), eq(MESSAGE_CODE),
        eq(Locale.getDefault())))
        .thenReturn(MESSAGE_FROM_PROPERTIES);

    //when
    List<String> result = messageService.getMessages(List.of(MESSAGE_CODE));

    //then
    Assertions.assertEquals(MESSAGE_FROM_PROPERTIES, result.get(0));
  }
}
