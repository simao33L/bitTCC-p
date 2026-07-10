package com.bat.uberlandia.dashboard.repository;

import java.time.LocalDate;

public interface TendenciaDiaria {
    LocalDate getDia();
    Long getTotal();
}