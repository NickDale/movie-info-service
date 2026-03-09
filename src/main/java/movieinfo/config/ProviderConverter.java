package movieinfo.config;

import lombok.NonNull;
import movieinfo.controller.model.Provider;
import movieinfo.exception.InvalidProviderException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ProviderConverter implements Converter<String, Provider> {

    @Override
    public Provider convert(@NonNull String source) {
        try {
            return Provider.valueOf(source.trim().toUpperCase());
        } catch (Exception ex) {
            throw new InvalidProviderException("Invalid provider: " + source, ex);
        }
    }
}
