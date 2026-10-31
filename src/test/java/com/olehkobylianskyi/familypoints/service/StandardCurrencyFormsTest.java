package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.PointType;
import com.olehkobylianskyi.familypoints.entity.PointTypeNameForm;
import com.olehkobylianskyi.familypoints.entity.Workspace;
import com.olehkobylianskyi.familypoints.repository.PointTypeNameFormRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StandardCurrencyFormsTest {
    private final PointTypeNameFormRepository repository = mock(PointTypeNameFormRepository.class);
    private final StandardCurrencyForms defaults = new StandardCurrencyForms(repository);

    @Test void insertsFourLanguagesForStandardCurrency() {
        var type = mock(PointType.class);
        when(type.getId()).thenReturn(17L);
        when(type.getCode()).thenReturn("GOLD");
        when(repository.save(any(PointTypeNameForm.class))).thenAnswer(i -> i.getArgument(0));

        defaults.ensureFor(type);

        verify(repository, times(4)).save(any(PointTypeNameForm.class));
        verify(repository).save(argThat(form ->
                form.getLanguage().equals("uk") && form.getOne().equals("золота монета")
                    && form.getFew().equals("золоті монети") && form.getMany().equals("золотих монет")));
        verify(repository).save(argThat(form ->
                form.getLanguage().equals("de") && form.getOne().equals("Goldmünze")));
    }

    @Test void preservesCustomFormsAndDoesNotDuplicate() {
        var type = mock(PointType.class);
        when(type.getId()).thenReturn(18L);
        when(type.getCode()).thenReturn("POINTS");
        var customized = new PointTypeNameForm(type, "uk", "зірка", "зірки", "зірок");
        when(repository.findByPointTypeIdAndLanguage(18L, "uk")).thenReturn(Optional.of(customized));

        defaults.ensureFor(type);

        assertEquals("зірка", customized.getOne());
        verify(repository, times(3)).save(any(PointTypeNameForm.class));
        verify(repository, never()).save(same(customized));
    }

    @Test void ignoresCustomCurrencies() {
        var custom = new PointType(mock(Workspace.class), "CRYSTALS", "Crystals", 100);
        defaults.ensureFor(custom);
        verifyNoInteractions(repository);
    }
}
