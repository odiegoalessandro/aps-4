package com.aps.weather.demo.service;

import com.aps.weather.demo.dao.LocalizacaoDao;
import com.aps.weather.demo.dao.PrevisaoDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import com.aps.weather.demo.domain.PrevisaoDoCLima;
import com.aps.weather.demo.exception.CidadeNaoEncontradaException;
import com.aps.weather.demo.exception.RequisicaoInvalidaException;
import com.aps.weather.demo.exception.SemDadosClimaticosException;
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

    private final LocalizacaoDao localizacaoDao;
    private final PrevisaoDao previsaoDao;

    public WeatherQueryService(LocalizacaoDao localizacaoDao, PrevisaoDao previsaoDao) {
        this.localizacaoDao = localizacaoDao;
        this.previsaoDao = previsaoDao;
    }

    public List<Localizacao> listarCidades() {
        return localizacaoDao.listar();
    }

    public Localizacao buscarCidade(String cidade) {
        String nome = validarCidade(cidade);
        return localizacaoDao.buscarPorNomeNormalizado(NomeDeCidade.normalizar(nome))
                .orElseThrow(() -> new CidadeNaoEncontradaException(nome));
    }

    public Localizacao buscarCidadePorId(Long id) {
        return localizacaoDao.buscarPorId(id)
                .orElseThrow(() -> CidadeNaoEncontradaException.paraId(id));
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
        return ultimaPrevisao(buscarCidade(cidade));
    }

    public PrevisaoDoCLima ultimaPrevisao(Localizacao local) {
        return previsaoDao.ultimaDe(local)
                .orElseThrow(() -> new SemDadosClimaticosException(local.getCity()));
    }

    public Page<PrevisaoDoCLima> historico(String cidade, int page, int size) {
        validarPaginacao(page, size);
        return paginarHistorico(buscarCidade(cidade), page, size);
    }

    public Page<PrevisaoDoCLima> historico(Localizacao local, int page, int size) {
        validarPaginacao(page, size);
        return paginarHistorico(local, page, size);
    }

    private Page<PrevisaoDoCLima> paginarHistorico(Localizacao local, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, TAMANHO_MAXIMO_PAGINA), ORDEM_HISTORICO);
        return previsaoDao.historicoDe(local, pageable);
    }

    private static void validarPaginacao(int page, int size) {
        if (page < 0) {
            throw new RequisicaoInvalidaException("O parâmetro page não pode ser negativo");
        }
        if (size < 1) {
            throw new RequisicaoInvalidaException("O parâmetro size deve ser maior que zero");
        }
    }
}
