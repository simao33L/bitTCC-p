package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Chamado;
import java.time.LocalDate;

public interface TendenciaMotivoDiaria {
    LocalDate getDia();
    Chamado.MotivoFalha getMotivo();
    Long getTotal();
}