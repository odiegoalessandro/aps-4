package com.aps.weather.demo.dao.jpa;

import com.aps.weather.demo.dao.LocalizacaoDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.repository.LocalizacaoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaLocalizacaoDao implements LocalizacaoDao {

    private final LocalizacaoRepository repository;

    public JpaLocalizacaoDao(LocalizacaoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Localizacao> listar() {
        return repository.findAllByOrderByCityAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Localizacao> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Localizacao> buscarPorNomeNormalizado(String nomeNormalizado) {
        return repository.findByCityNormalizada(nomeNormalizado);
    }

    @Override
    @Transactional
    public Localizacao salvar(Localizacao localizacao) {
        return repository.save(localizacao);
    }

    @Override
    @Transactional
    public Localizacao salvarEFlushar(Localizacao localizacao) {
        return repository.saveAndFlush(localizacao);
    }
}
