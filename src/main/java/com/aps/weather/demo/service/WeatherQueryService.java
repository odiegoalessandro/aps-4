package com.aps.weather.demo.service;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import com.aps.weather.demo.repository.PrevisaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WeatherQueryService {

    /** Limite da coluna locations.city. */
    private static final int TAMANHO_MAXIMO_CIDADE = 150;

    /** Teto de itens por página no histórico. */
    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private static final Sort ORDEM_HISTORICO =
            Sort.by(Sort.Order.desc("forecastAt"), Sort.Order.desc("id"));

    private final LocalizacaoRepository localizacaoRepository;
    private final PrevisaoRepository previsaoRepository;

    public WeatherQueryService(LocalizacaoRepository localizacaoRepository,
                               PrevisaoRepository previsaoRepository) {
        this.localizacaoRepository = localizacaoRepository;
        this.previsaoRepository = previsaoRepository;
    }

    public List<Localizacao> listarCidades() {
        return localizacaoRepository.findAllByOrderByCityAsc();
    }

    public Localizacao buscarCidade(String cidade) {
        String nome = validarCidade(cidade);
        return localizacaoRepository.findByCityNormalizada(NomeDeCidade.normalizar(nome))
                .orElseThrow(() -> new CidadeNaoEncontradaException(nome));
    }

    private static String validarCidade(String cidade) {
        String nome = cidade.trim();
        if (nome.isEmpty()) {
            throw new RequisicaoInvalidaException("O nome da cidade não pode estar vazio");
        }
        if (nome.length() > TAMANHO_MAXIMO_CIDADE) {
            throw new RequisicaoInvalidaException(
                    "O nome da cidade deve ter no máximo " + TAMANHO_MAXIMO_CIDADE + " caracteres");
        }
        return nome;
    }

    public PrevisaoDoCLima ultimaPrevisao(String cidade) {
        Localizacao local = buscarCidade(cidade);
        return previsaoRepository.findFirstByLocationOrderByForecastAtDesc(local)
                .orElseThrow(() -> new SemDadosClimaticosException(cidade));
    }

    public Page<PrevisaoDoCLima> historico(String cidade, int page, int size) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("O parâmetro page não pode ser negativo");
        }
        if (size < 1) {
            throw new RequisicaoInvalidaException("O parâmetro size deve ser maior que zero");
        }

        Localizacao local = buscarCidade(cidade);
        Pageable pageable = PageRequest.of(page, Math.min(size, TAMANHO_MAXIMO_PAGINA), ORDEM_HISTORICO);
        return previsaoRepository.findByLocation(local, pageable);
    }
}
