package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;

import java.util.List;
import java.util.Optional;

/**
 * Acesso às localizações cadastradas. A busca usa a forma normalizada de
 * {@link com.aps.weather.demo.domain.NomeDeCidade}, que ignora caixa e acentos.
 */
public interface LocalizacaoDao {

    List<Localizacao> listar();

    Optional<Localizacao> buscarPorId(Long id);

    Optional<Localizacao> buscarPorNomeNormalizado(String nomeNormalizado);

    Localizacao salvar(Localizacao localizacao);

    Localizacao salvarEFlushar(Localizacao localizacao);
}
