package com.aps.weather.demo.api;

import com.aps.weather.demo.api.dto.HistoricoResponse;
import com.aps.weather.demo.api.dto.PrevisaoDiariaResponse;
import com.aps.weather.demo.api.dto.PrevisaoHorariaResponse;
import com.aps.weather.demo.api.dto.PrevisaoResponse;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.service.ColetaService;
import com.aps.weather.demo.service.PrevisaoService;
import com.aps.weather.demo.service.WeatherQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas equivalentes às de {@code /api/weather/{city}} e {@code /api/collect/{city}},
 * mas identificando a cidade pelo id devolvido em {@code GET /api/cities}. O id
 * elimina a necessidade de codificar o nome na URL (acentos e espaços).
 */
@RestController
@RequestMapping("/api/cities/{id}")
public class CityWeatherController {

    private final WeatherQueryService weatherQueryService;
    private final PrevisaoService previsaoService;
    private final ColetaService coletaService;

    public CityWeatherController(WeatherQueryService weatherQueryService,
                                 PrevisaoService previsaoService,
                                 ColetaService coletaService) {
        this.weatherQueryService = weatherQueryService;
        this.previsaoService = previsaoService;
        this.coletaService = coletaService;
    }

    @GetMapping("/weather")
    public PrevisaoResponse ultima(@PathVariable Long id) {
        return PrevisaoResponse.from(weatherQueryService.ultimaPrevisao(local(id)));
    }

    @GetMapping("/history")
    public HistoricoResponse historico(@PathVariable Long id,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return HistoricoResponse.from(weatherQueryService.historico(local(id), page, size));
    }

    @GetMapping("/hourly")
    public PrevisaoHorariaResponse horarias(@PathVariable Long id) throws Exception {
        return PrevisaoHorariaResponse.from(previsaoService.horarias(local(id)));
    }

    @GetMapping("/daily")
    public PrevisaoDiariaResponse diarias(@PathVariable Long id) throws Exception {
        return PrevisaoDiariaResponse.from(previsaoService.diarias(local(id)));
    }

    @PostMapping("/collect")
    public List<PrevisaoResponse> coletar(@PathVariable Long id) throws Exception {
        return coletaService.coletarCidade(local(id)).stream()
                .map(PrevisaoResponse::from)
                .toList();
    }

    private Localizacao local(Long id) {
        return weatherQueryService.buscarCidadePorId(id);
    }
}
