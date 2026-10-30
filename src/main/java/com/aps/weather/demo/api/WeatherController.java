package com.aps.weather.demo.api;

import com.aps.weather.demo.api.dto.HistoricoResponse;
import com.aps.weather.demo.api.dto.PrevisaoResponse;
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

    public WeatherController(WeatherQueryService weatherQueryService) {
        this.weatherQueryService = weatherQueryService;
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
}
