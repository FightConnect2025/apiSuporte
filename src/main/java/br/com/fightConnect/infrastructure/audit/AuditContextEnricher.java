package br.com.fightConnect.infrastructure.audit;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AuditContextEnricher {

    public Map<String, Object> enrichWithEntity(String entidade, Object result, Map<String, Object> existingDados) {
        Map<String, Object> enriched = new HashMap<>(existingDados);

        if (result == null) return enriched;

        try {
            switch (entidade) {
                case "ALUNO":
                    enriched.put("aluno", extractAlunoData(result));
                    break;
                case "PROFESSOR":
                    enriched.put("professor", extractProfessorData(result));
                    break;
                case "EQUIPE":
                    enriched.put("equipe", extractEquipeData(result));
                    break;
                case "TURMA":
                    enriched.put("turma", extractTurmaData(result));
                    break;
                case "CAMPEONATO":
                    enriched.put("campeonato", extractCampeonatoData(result));
                    break;
                case "PRESENCA":
                    enriched.put("presenca", extractPresencaData(result));
                    break;
                case "MEDALHA":
                    enriched.put("medalha", extractMedalhaData(result));
                    break;
                case "FAIXA":
                    enriched.put("faixa", extractFaixaData(result));
                    break;
                case "CARTEIRA_COMPETIDOR":
                    enriched.put("carteira", extractCarteiraData(result));
                    break;
                case "SUPORTE_TICKET":
                    enriched.put("ticket", extractTicketData(result));
                    break;
                case "MATRICULA_FINANCEIRA":
                    enriched.put("matricula", extractMatriculaData(result));
                    break;
                case "PLANO_ALUNO":
                    enriched.put("plano", extractPlanoData(result));
                    break;
                case "PAGAMENTO":
                    enriched.put("pagamento", extractPagamentoData(result));
                    break;
                case "ASSINATURA_EQUIPE":
                    enriched.put("assinatura", extractAssinaturaData(result));
                    break;
            }
        } catch (Exception e) {
            enriched.put("enrichmentError", "Falha ao enriquecer contexto: " + e.getMessage());
        }

        return enriched;
    }

    private Map<String, Object> extractAlunoData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "nomeCompleto");
            putIfHasGetter(data, result, clazz, "cpf");
            putIfHasGetter(data, result, clazz, "rg");
            putIfHasGetter(data, result, clazz, "email");
            putIfHasGetter(data, result, clazz, "telefone");
            putIfHasGetter(data, result, clazz, "telefoneEmergencia");
            putIfHasGetter(data, result, clazz, "contatoEmergencia");
            putIfHasGetter(data, result, clazz, "dataNascimento");
            putIfHasGetter(data, result, clazz, "sexo");
            putIfHasGetter(data, result, clazz, "graduacao");
            putIfHasGetter(data, result, clazz, "graduacaoAtual");
            putIfHasGetter(data, result, clazz, "peso");
            putIfHasGetter(data, result, clazz, "altura");
            putIfHasGetter(data, result, clazz, "tipoSanguineo");
            putIfHasGetter(data, result, clazz, "planoSaude");
            putIfHasGetter(data, result, clazz, "alergias");
            putIfHasGetter(data, result, clazz, "condicoesMedicas");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "equipe");
            putIfHasGetter(data, result, clazz, "professorResponsavel");
            putIfHasGetter(data, result, clazz, "turmas");
            putIfHasGetter(data, result, clazz, "endereco");
            putIfHasGetter(data, result, clazz, "fotoUrl");
            putIfHasGetter(data, result, clazz, "dataCadastro");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractProfessorData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "email");
            putIfHasGetter(data, result, clazz, "telefone");
            putIfHasGetter(data, result, clazz, "cpf");
            putIfHasGetter(data, result, clazz, "graduacao");
            putIfHasGetter(data, result, clazz, "perfil");
            putIfHasGetter(data, result, clazz, "equipe");
            putIfHasGetter(data, result, clazz, "turmas");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "dataCadastro");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractEquipeData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "mestre");
            putIfHasGetter(data, result, clazz, "unidade");
            putIfHasGetter(data, result, clazz, "endereco");
            putIfHasGetter(data, result, clazz, "telefone");
            putIfHasGetter(data, result, clazz, "email");
            putIfHasGetter(data, result, clazz, "logoUrl");
            putIfHasGetter(data, result, clazz, "bannerUrl");
            putIfHasGetter(data, result, clazz, "status");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractTurmaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "modalidade");
            putIfHasGetter(data, result, clazz, "nivel");
            putIfHasGetter(data, result, clazz, "horarios");
            putIfHasGetter(data, result, clazz, "professor");
            putIfHasGetter(data, result, clazz, "local");
            putIfHasGetter(data, result, clazz, "capacidadeMaxima");
            putIfHasGetter(data, result, clazz, "alunosMatriculados");
            putIfHasGetter(data, result, clazz, "equipe");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractCampeonatoData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "dataInicio");
            putIfHasGetter(data, result, clazz, "dataFim");
            putIfHasGetter(data, result, clazz, "local");
            putIfHasGetter(data, result, clazz, "endereco");
            putIfHasGetter(data, result, clazz, "organizador");
            putIfHasGetter(data, result, clazz, "modalidades");
            putIfHasGetter(data, result, clazz, "status");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractPresencaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "turma");
            putIfHasGetter(data, result, clazz, "data");
            putIfHasGetter(data, result, clazz, "horario");
            putIfHasGetter(data, result, clazz, "presente");
            putIfHasGetter(data, result, clazz, "observacao");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractMedalhaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "campeonato");
            putIfHasGetter(data, result, clazz, "tipo");
            putIfHasGetter(data, result, clazz, "categoria");
            putIfHasGetter(data, result, clazz, "graduacao");
            putIfHasGetter(data, result, clazz, "data");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractFaixaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "graduacao");
            putIfHasGetter(data, result, clazz, "dataGraduacao");
            putIfHasGetter(data, result, clazz, "professor");
            putIfHasGetter(data, result, clazz, "observacao");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractCarteiraData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "competidor");
            putIfHasGetter(data, result, clazz, "numeroCarteira");
            putIfHasGetter(data, result, clazz, "validade");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "graduacao");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractTicketData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "numero");
            putIfHasGetter(data, result, clazz, "titulo");
            putIfHasGetter(data, result, clazz, "descricao");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "prioridade");
            putIfHasGetter(data, result, clazz, "solicitante");
            putIfHasGetter(data, result, clazz, "responsavel");
            putIfHasGetter(data, result, clazz, "equipe");
            putIfHasGetter(data, result, clazz, "mensagens");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractMatriculaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "plano");
            putIfHasGetter(data, result, clazz, "valor");
            putIfHasGetter(data, result, clazz, "diaVencimento");
            putIfHasGetter(data, result, clazz, "formaPagamento");
            putIfHasGetter(data, result, clazz, "desconto");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "dataInicio");
            putIfHasGetter(data, result, clazz, "dataFim");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractPlanoData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "nome");
            putIfHasGetter(data, result, clazz, "descricao");
            putIfHasGetter(data, result, clazz, "valor");
            putIfHasGetter(data, result, clazz, "periodicidade");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "status");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractPagamentoData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "aluno");
            putIfHasGetter(data, result, clazz, "valor");
            putIfHasGetter(data, result, clazz, "dataVencimento");
            putIfHasGetter(data, result, clazz, "dataPagamento");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "formaPagamento");
            putIfHasGetter(data, result, clazz, "nossoNumero");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private Map<String, Object> extractAssinaturaData(Object result) {
        Map<String, Object> data = new HashMap<>();
        try {
            var clazz = result.getClass();
            putIfHasGetter(data, result, clazz, "id");
            putIfHasGetter(data, result, clazz, "equipe");
            putIfHasGetter(data, result, clazz, "plano");
            putIfHasGetter(data, result, clazz, "valor");
            putIfHasGetter(data, result, clazz, "status");
            putIfHasGetter(data, result, clazz, "dataInicio");
            putIfHasGetter(data, result, clazz, "dataFim");
            putIfHasGetter(data, result, clazz, "dataCancelamento");
        } catch (Exception e) {
            data.put("extractError", e.getMessage());
        }
        return data;
    }

    private void putIfHasGetter(Map<String, Object> data, Object obj, Class<?> clazz, String field) {
        try {
            String getterName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
            var method = clazz.getMethod(getterName);
            Object value = method.invoke(obj);
            if (value != null) {
                data.put(field, value);
            }
        } catch (NoSuchMethodException ignored) {
            try {
                String getterName = "is" + field.substring(0, 1).toUpperCase() + field.substring(1);
                var method = clazz.getMethod(getterName);
                Object value = method.invoke(obj);
                if (value != null) {
                    data.put(field, value);
                }
            } catch (Exception ignored2) {
            }
        } catch (Exception ignored) {
        }
    }
}
