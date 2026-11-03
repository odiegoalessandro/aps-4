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
                new Localizacao("Porto Alegre", -30.03, -51.23, "America/Sao_Paulo"),

                // Principais cidades do estado de São Paulo.
                new Localizacao("Barueri", -23.5106, -46.8761, "America/Sao_Paulo"),
                new Localizacao("Bauru", -22.3147, -49.0606, "America/Sao_Paulo"),
                new Localizacao("Campinas", -22.9056, -47.0608, "America/Sao_Paulo"),
                new Localizacao("Diadema", -23.6861, -46.6228, "America/Sao_Paulo"),
                new Localizacao("Franca", -20.5386, -47.4008, "America/Sao_Paulo"),
                new Localizacao("Guarulhos", -23.4628, -46.5333, "America/Sao_Paulo"),
                new Localizacao("Jundiaí", -23.1864, -46.8842, "America/Sao_Paulo"),
                new Localizacao("Limeira", -22.5647, -47.4017, "America/Sao_Paulo"),
                new Localizacao("Marília", -22.2139, -49.9458, "America/Sao_Paulo"),
                new Localizacao("Mauá", -23.6678, -46.4614, "America/Sao_Paulo"),
                new Localizacao("Osasco", -23.5325, -46.7917, "America/Sao_Paulo"),
                new Localizacao("Piracicaba", -22.7253, -47.6492, "America/Sao_Paulo"),
                new Localizacao("Praia Grande", -24.0058, -46.4028, "America/Sao_Paulo"),
                new Localizacao("Ribeirão Preto", -21.1775, -47.8103, "America/Sao_Paulo"),
                new Localizacao("Santo André", -23.6639, -46.5383, "America/Sao_Paulo"),
                new Localizacao("Santos", -23.9608, -46.3336, "America/Sao_Paulo"),
                new Localizacao("São Bernardo do Campo", -23.6939, -46.5650, "America/Sao_Paulo"),
                new Localizacao("São José do Rio Preto", -20.8197, -49.3794, "America/Sao_Paulo"),
                new Localizacao("São José dos Campos", -23.1794, -45.8869, "America/Sao_Paulo"),
                new Localizacao("Sorocaba", -23.5017, -47.4581, "America/Sao_Paulo"),
                new Localizacao("Suzano", -23.5425, -46.3108, "America/Sao_Paulo"),
                new Localizacao("Taubaté", -23.0264, -45.5553, "America/Sao_Paulo"));
    }
}
