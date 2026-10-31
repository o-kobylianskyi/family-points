package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.PointType;
import com.olehkobylianskyi.familypoints.entity.PointTypeNameForm;
import com.olehkobylianskyi.familypoints.repository.PointTypeNameFormRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Persisted defaults for standard currencies; existing customized forms always win. */
@Component
public class StandardCurrencyForms {
    private final PointTypeNameFormRepository repository;

    private static final Map<String, Map<String, String[]>> DEFAULTS = Map.of(
        "POINTS", Map.of(
            "uk", new String[]{"бал", "бали", "балів"},
            "ru", new String[]{"балл", "балла", "баллов"},
            "en", new String[]{"point", "points", "points"},
            "de", new String[]{"Punkt", "Punkte", "Punkte"}),
        "COPPER", Map.of(
            "uk", new String[]{"мідна монета", "мідні монети", "мідних монет"},
            "ru", new String[]{"медная монета", "медные монеты", "медных монет"},
            "en", new String[]{"copper coin", "copper coins", "copper coins"},
            "de", new String[]{"Kupfermünze", "Kupfermünzen", "Kupfermünzen"}),
        "SILVER", Map.of(
            "uk", new String[]{"срібна монета", "срібні монети", "срібних монет"},
            "ru", new String[]{"серебряная монета", "серебряные монеты", "серебряных монет"},
            "en", new String[]{"silver coin", "silver coins", "silver coins"},
            "de", new String[]{"Silbermünze", "Silbermünzen", "Silbermünzen"}),
        "GOLD", Map.of(
            "uk", new String[]{"золота монета", "золоті монети", "золотих монет"},
            "ru", new String[]{"золотая монета", "золотые монеты", "золотых монет"},
            "en", new String[]{"gold coin", "gold coins", "gold coins"},
            "de", new String[]{"Goldmünze", "Goldmünzen", "Goldmünzen"})
    );

    public StandardCurrencyForms(PointTypeNameFormRepository repository) {
        this.repository = repository;
    }

    public void ensureFor(PointType type) {
        Map<String, String[]> forms = DEFAULTS.get(type.getCode());
        if (forms == null) return;
        forms.forEach((language, words) -> {
            if (repository.findByPointTypeIdAndLanguage(type.getId(), language).isEmpty()) {
                repository.save(new PointTypeNameForm(type, language, words[0], words[1], words[2]));
            }
        });
    }
}
