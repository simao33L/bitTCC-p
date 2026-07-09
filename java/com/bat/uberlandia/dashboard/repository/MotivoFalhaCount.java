package com.bat.uberlandia.dashboard.repository;

import com.bat.uberlandia.dashboard.model.Chamado;

public interface MotivoFalhaCount {
    Chamado.MotivoFalha getMotivo();
    Long getTotal();
}
