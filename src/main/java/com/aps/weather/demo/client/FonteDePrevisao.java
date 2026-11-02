package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDiaria;
import com.aps.weather.demo.domain.PrevisaoHoraria;

import java.util.List;

public interface FonteDePrevisao {

    List<PrevisaoHoraria> buscarHorarias(Localizacao local) throws Exception;

    List<PrevisaoDiaria> buscarDiarias(Localizacao local) throws Exception;
}
