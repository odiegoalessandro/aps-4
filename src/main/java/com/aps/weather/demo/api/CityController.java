package com.aps.weather.demo.api;

import com.aps.weather.demo.api.dto.LocalizacaoResponse;
import com.aps.weather.demo.service.WeatherQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final WeatherQueryService weatherQueryService;

    public CityController(WeatherQueryService weatherQueryService) {
        this.weatherQueryService = weatherQueryService;
    }

    @GetMapping
    public List<LocalizacaoResponse> listar() {
        return weatherQueryService.listarCidades().stream()
                .map(LocalizacaoResponse::from)
                .toList();
    }
}
