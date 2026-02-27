package br.com.fightConnect.infrastructure.clients.apiAuth.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessorResponseDtoClient {

    @JsonAlias({ "id", "Id", "usuarioId", "UsuarioId" })
    private UUID id;

    @JsonAlias({ "email", "Email" })
    private String email;

    @JsonAlias({ "nome", "Nome", "name" })
    private String nome;

    @JsonAlias({ "equipeId", "EquipeId" })
    private UUID equipeId;

    @JsonAlias({ "planoId", "PlanoId" })
    private UUID planoId;

    @JsonAlias({ "nomeEquipe", "NomeEquipe" })
    private String nomeEquipe;

    @JsonAlias({ "plano", "Plano", "nomePlano", "NomePlano" })
    private String nomePlano;

    @JsonAlias({ "tokenPush", "TokenPush" })
    private String tokenPush;
}
