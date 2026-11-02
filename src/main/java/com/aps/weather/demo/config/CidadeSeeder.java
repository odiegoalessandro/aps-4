package com.aps.weather.demo.config;

import com.aps.weather.demo.dao.LocalizacaoDao;
import com.aps.weather.demo.domain.Localizacao;
import com.aps.weather.demo.domain.NomeDeCidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CidadeSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CidadeSeeder.class);

    private final LocalizacaoDao localizacaoDao;

    public CidadeSeeder(LocalizacaoDao localizacaoDao) {
        this.localizacaoDao = localizacaoDao;
    }

    @Override
    public void run(ApplicationArguments args) {
        int novas = 0;
        for (Localizacao cidade : cidadesIniciais()) {
            if (localizacaoDao.buscarPorNomeNormalizado(
                    NomeDeCidade.normalizar(cidade.getCity())).isEmpty()) {
                localizacaoDao.salvar(cidade);
                novas++;
            }
        }
        if (novas > 0) {
            LOG.info("Seed de cidades: {} nova(s) cidade(s) cadastrada(s)", novas);
        }
    }

    // instancias novas a cada execução: entidades não devem ser reutilizadas entre contextos
    private static List<Localizacao> cidadesIniciais() {
        return List.of(
                new Localizacao("São Paulo", -23.55, -46.63, "America/Sao_Paulo"),
                new Localizacao("Rio de Janeiro", -22.91, -43.17, "America/Sao_Paulo"),
                new Localizacao("Brasília", -15.79, -47.88, "America/Sao_Paulo"),
                new Localizacao("Salvador", -12.97, -38.50, "America/Sao_Paulo"),
                new Localizacao("Porto Alegre", -30.03, -51.23, "America/Sao_Paulo"));
    }
}
