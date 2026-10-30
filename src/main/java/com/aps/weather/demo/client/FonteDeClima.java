package com.aps.weather.demo.client;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.PrevisaoDoCLima;

public interface FonteDeClima {

    PrevisaoDoCLima buscar(Localizacao local) throws Exception;
}
