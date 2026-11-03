package com.aps.weather.demo.api;

import com.aps.weather.demo.api.dto.PrevisaoResponse;
import com.aps.weather.demo.api.dto.ResultadoColetaResponse;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.service.ColetaService;
import com.aps.weather.demo.service.ResultadoColeta;
import com.aps.weather.demo.service.WeatherQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collect")
public class CollectController {

    private final ColetaService coletaService;
    private final WeatherQueryService weatherQueryService;

    public CollectController(ColetaService coletaService,
                             WeatherQueryService weatherQueryService) {
        this.coletaService = coletaService;
        this.weatherQueryService = weatherQueryService;
    }

    @PostMapping
    public ResponseEntity<ResultadoColetaResponse> coletarTodas() {
        ResultadoColeta resultado = coletaService.coletarTodas();
        return ResponseEntity.status(statusDe(resultado))
                .body(ResultadoColetaResponse.from(resultado));
    }

    private static HttpStatus statusDe(ResultadoColeta resultado) {
        if (resultado.falhas().isEmpty()) {
            return HttpStatus.OK;
        }
        // Houve tentativas e todas falharam: a fonte está indisponível.
        if (resultado.gravadas().isEmpty()) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.MULTI_STATUS;
    }

    @PostMapping("/{city}")
    public List<PrevisaoResponse> coletarCidade(@PathVariable String city) throws Exception {
        Localizacao local = weatherQueryService.buscarCidade(city);
        return coletar(local);
    }

    @PostMapping("/id/{id}")
    public List<PrevisaoResponse> coletarCidadePorId(@PathVariable Long id) throws Exception {
        Localizacao local = weatherQueryService.buscarCidadePorId(id);
        return coletar(local);
    }

    private List<PrevisaoResponse> coletar(Localizacao local) throws Exception {
        return coletaService.coletarCidade(local).stream()
                .map(PrevisaoResponse::from)
                .toList();
    }
}
