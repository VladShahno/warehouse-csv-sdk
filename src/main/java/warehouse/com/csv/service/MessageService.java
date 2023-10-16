package warehouse.com.csv.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String getMessage(String messageCode, Object... args) {
    return messageSource.getMessage(messageCode, args, messageCode, Locale.getDefault());
  }

  public List<String> getMessages(List<String> messageCodes) {
    return messageCodes.stream()
        .map(this::getMessage)
        .collect(Collectors.toList());
  }

}
