package com.aps.weather.demo.dao;

import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão da troca dos repositories por DAO: a busca normalizada, a ordenação
 * e a gravação continuam valendo contra o SQLite real.
 */
@SpringBootTest
@Transactional
class LocalizacaoDaoTest {

    @Autowired
    private LocalizacaoDao dao;

    @Test
    void buscaNormalizadaIgnoraCaixaEAcentos() {
        dao.salvarEFlushar(new Localizacao("Cidade Dao Acentuada", -1.0, -2.0, "UTC"));

        assertThat(dao.buscarPorNomeNormalizado(
                NomeDeCidade.normalizar("CIDADE DAO AÇENTUADA")))
                .get()
                .extracting(Localizacao::getCity)
                .isEqualTo("Cidade Dao Acentuada");
    }

    @Test
    void buscaDeCidadeInexistenteRetornaVazio() {
        assertThat(dao.buscarPorNomeNormalizado(
                NomeDeCidade.normalizar("Cidade Dao Inexistente")))
                .isEmpty();
    }

    @Test
    void listarMantemOrdemAlfabetica() {
        dao.salvarEFlushar(new Localizacao("Cidade Dao Ordem B", -1.0, -2.0, "UTC"));
        dao.salvarEFlushar(new Localizacao("Cidade Dao Ordem A", -1.0, -2.0, "UTC"));

        List<String> nomes = dao.listar().stream()
                .map(Localizacao::getCity)
                .filter(cidade -> cidade.startsWith("Cidade Dao Ordem"))
                .toList();

        assertThat(nomes).containsExactly("Cidade Dao Ordem A", "Cidade Dao Ordem B");
    }

    @Test
    void buscaPorIdEncontraACidadeGravada() {
        Localizacao salva = dao.salvarEFlushar(
                new Localizacao("Cidade Dao Por Id", -5.0, -6.0, "UTC"));

        assertThat(dao.buscarPorId(salva.getId()))
                .get()
                .extracting(Localizacao::getCity)
                .isEqualTo("Cidade Dao Por Id");
    }

    @Test
    void buscaPorIdInexistenteRetornaVazio() {
        assertThat(dao.buscarPorId(-1L)).isEmpty();
    }

    @Test
    void salvarAtribuiIdEPersisteAoCidade() {
        Localizacao salva = dao.salvar(
                new Localizacao("Cidade Dao Salva", -3.0, -4.0, "America/Sao_Paulo"));

        assertThat(salva.getId()).isNotNull();
        assertThat(dao.buscarPorNomeNormalizado("cidade dao salva")).isPresent();
    }
}
