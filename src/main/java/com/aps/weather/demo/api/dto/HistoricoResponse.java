package com.aps.weather.demo.api.dto;

import com.aps.weather.demo.domain.PrevisaoDoCLima;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Formato estável para o histórico paginado. Serializar o Page direto expõe
 * detalhes internos do PageImpl (sort, pageable, first, last...), que não fazem
 * parte do contrato da API.
 */
public record HistoricoResponse(
        List<PrevisaoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static HistoricoResponse from(Page<PrevisaoDoCLima> pagina) {
        return new HistoricoResponse(
                pagina.getContent().stream().map(PrevisaoResponse::from).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
