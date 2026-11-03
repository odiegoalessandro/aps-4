package com.aps.weather.demo.api;

import com.aps.weather.demo.api.dto.HistoricoResponse;
import com.aps.weather.demo.api.dto.PrevisaoDiariaResponse;
import com.aps.weather.demo.api.dto.PrevisaoHorariaResponse;
import com.aps.weather.demo.api.dto.PrevisaoResponse;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.service.PrevisaoService;
import com.aps.weather.demo.service.WeatherQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherQueryService weatherQueryService;
    private final PrevisaoService previsaoService;

    public WeatherController(WeatherQueryService weatherQueryService,
                             PrevisaoService previsaoService) {
        this.weatherQueryService = weatherQueryService;
        this.previsaoService = previsaoService;
    }

    @GetMapping("/{city}")
    public PrevisaoResponse ultima(@PathVariable String city) {
        return PrevisaoResponse.from(weatherQueryService.ultimaPrevisao(city));
    }

    @GetMapping("/{city}/history")
    public HistoricoResponse historico(@PathVariable String city,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        return HistoricoResponse.from(weatherQueryService.historico(city, page, size));
    }

    @GetMapping("/{city}/hourly")
    public PrevisaoHorariaResponse horarias(@PathVariable String city) throws Exception {
        return PrevisaoHorariaResponse.from(previsaoService.horarias(city));
    }

    @GetMapping("/{city}/daily")
    public PrevisaoDiariaResponse diarias(@PathVariable String city) throws Exception {
        return PrevisaoDiariaResponse.from(previsaoService.diarias(city));
    }

    @GetMapping("/id/{id}")
    public PrevisaoResponse ultimaPorId(@PathVariable Long id) {
        return PrevisaoResponse.from(weatherQueryService.ultimaPrevisao(local(id)));
    }

    @GetMapping("/id/{id}/history")
    public HistoricoResponse historicoPorId(@PathVariable Long id,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return HistoricoResponse.from(weatherQueryService.historico(local(id), page, size));
    }

    @GetMapping("/id/{id}/hourly")
    public PrevisaoHorariaResponse horariasPorId(@PathVariable Long id) throws Exception {
        return PrevisaoHorariaResponse.from(previsaoService.horarias(local(id)));
    }

    @GetMapping("/id/{id}/daily")
    public PrevisaoDiariaResponse diariasPorId(@PathVariable Long id) throws Exception {
        return PrevisaoDiariaResponse.from(previsaoService.diarias(local(id)));
    }

    private Localizacao local(Long id) {
        return weatherQueryService.buscarCidadePorId(id);
    }
}
